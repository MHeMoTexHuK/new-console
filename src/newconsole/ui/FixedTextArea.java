package newconsole.ui;

import arc.func.Cons;
import arc.graphics.g2d.Font;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.style.Drawable;
import arc.scene.ui.TextArea;
import arc.scene.ui.TextField;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/*
 * Was created to fix an arc-level bug that didn't allow to use custom fonts.
 * Will be made into a code-assisting text area.
 */
public class FixedTextArea extends TextArea {
	public FixedTextArea(String text) {
		super(text);
	}


	public FixedTextArea(String text, TextFieldStyle style) {
		super(text, style);
	}

	public void insertAtCursor(CharSequence newText) {
		insertAt(cursor, newText);
	}

	public void insertAt(int pos, CharSequence newText) {
		text = text.substring(0, pos) + newText + text.substring(pos);
		if (pos <= cursor) cursor += newText.length();
		updateDisplayText();
	}

	public void changed(Cons<String> listener) {
		changed(() -> listener.get(getText()));
	}

	@Override
	protected InputListener createInputListener() {
		return new AssistingInputListener();
	}

	@Override
	protected void updateDisplayText() {
		super.updateDisplayText();

		glyphPositions.clear();
		layout.setText(style.font, displayText.toString().replace('\n', ' ').replace('\r', ' '));
		var runs = layout.runs;

		if (runs.size > 0) {
			var xAdvances = runs.first().xAdvances;
			float x = 0;
			for (int j = 1; j < xAdvances.size; j++) {
				glyphPositions.add(x);
				x += xAdvances.get(j);
			}
			glyphPositions.add(x);
		}
	}

	@Override
	protected void drawSelection(Drawable selection, Font font, float x, float y) {
		int i = firstLineShowing * 2;
		float offsetY = 0;
		int minIndex = Math.min(cursor, selectionStart);
		int maxIndex = Math.max(cursor, selectionStart);
		while (i + 1 < linesBreak.size && i < (firstLineShowing + linesShowing) * 2) {

			int lineStart = linesBreak.get(i);
			int lineEnd = linesBreak.get(i + 1);

			if (!((minIndex < lineStart && minIndex < lineEnd && maxIndex < lineStart && maxIndex < lineEnd)
				|| (minIndex > lineStart && minIndex > lineEnd && maxIndex > lineStart && maxIndex > lineEnd))) {

				int start = Math.min(Math.max(linesBreak.get(i), minIndex), glyphPositions.size - 1);
				int end = Math.min(Math.min(linesBreak.get(i + 1), maxIndex), glyphPositions.size - 1);

				float selectionX = glyphPositions.get(start) - glyphPositions.get(Math.min(linesBreak.get(i), glyphPositions.size));
				float selectionWidth = glyphPositions.get(end) - glyphPositions.get(start);

				selection.draw(x + selectionX + fontOffset, y - textHeight - font.getDescent() - offsetY, selectionWidth,
					font.getLineHeight());
			}

			offsetY += font.getLineHeight();
			i += 2;
		}
	}

	class AssistingInputListener extends TextAreaListener {
		@Override
		public boolean keyTyped(InputEvent event, char character) {
			if (character == '\t') {
				insertAtCursor("    ");
				return true;
			} else if (character == '\n') {
				var oldText = text;
				var oldLine = cursorLine;

				if (super.keyTyped(event, character) && cursorLine > 0 && oldLine * 2 < linesBreak.size) {
					// determine how many spaces the previous line has had
					var i = linesBreak.get(oldLine * 2);
					var leadingSpace = new StringBuilder();
					while (i < oldText.length() && oldText.charAt(i++) == ' ')
						leadingSpace.append(" ");
					// insert the same amount of spaces
					insertAtCursor(leadingSpace);
				}
				return true;
			}

			return super.keyTyped(event, character);
		}
	}
}
