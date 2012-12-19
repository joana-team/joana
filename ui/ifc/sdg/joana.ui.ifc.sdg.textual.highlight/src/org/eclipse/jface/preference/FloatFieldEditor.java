/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 29.10.2005
 * @author kai brueckner
 * university of passau
 */
package org.eclipse.jface.preference;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class FloatFieldEditor extends StringFieldEditor {

    private float minValidValue = -Float.MAX_VALUE;
    private float maxValidValue = Float.MAX_VALUE;

    private static final int DEFAULT_TEXT_LIMIT = 10;

    /**
     * Creates a new integer field editor
     */
    protected FloatFieldEditor() {
    }

    /**
     * Creates an integer field editor.
     *
     * @param name the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param parent the parent of the field editor's control
     */
    public FloatFieldEditor(String name, String labelText, Composite parent) {
        this(name, labelText, DEFAULT_TEXT_LIMIT, parent);
    }

    /**
     * Creates an integer field editor.
     *
     * @param name the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param parent the parent of the field editor's control
     * @param textLimit the maximum number of characters in the text.
     */
    public FloatFieldEditor(String name, String labelText,int width ,Composite parent) {
        super(name,labelText,width,parent);
        setTextLimit(DEFAULT_TEXT_LIMIT);
        setEmptyStringAllowed(false);
        setErrorMessage("Value must be a float");//$NON-NLS-1$
    }

    /**
     * Creates an integer field editor.
     *
     * @param name the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param parent the parent of the field editor's control
     * @param textLimit the maximum number of characters in the text.
     * @param width width the width of the text input field in characters,
     *  or <code>UNLIMITED</code> for no limit
     */
    public FloatFieldEditor(String name, String labelText, Composite parent,
            int textLimit, int width) {
        super(name,labelText,width,parent);
        setTextLimit(textLimit);
        setEmptyStringAllowed(false);
        setErrorMessage("Value must be a float");//$NON-NLS-1$
    }

    /**
     * Sets the range of valid values for this field.
     *
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     */
    public void setValidRange(float min, float max) {
        minValidValue = min;
        maxValidValue = max;
    }

    /* (non-Javadoc)
     * Method declared on StringFieldEditor.
     * Checks whether the entered String is a valid integer or not.
     */
    protected boolean doCheckState() {

        Text text = getTextControl();

        if (text == null)
            return false;

        String numberString = text.getText();
        try {
            float number = Float.valueOf(numberString).floatValue();

            if (number >= minValidValue && number <= maxValidValue) {
                clearErrorMessage();

                return true;
            } else {
                showErrorMessage();
                return false;
            }
        } catch (NumberFormatException e1) {
            showErrorMessage();
        }

        return false;
    }

    /* (non-Javadoc)
     * Method declared on FieldEditor.
     */
    protected void doLoad() {
        Text text = getTextControl();
        if (text != null) {
            float value = getPreferenceStore().getFloat(getPreferenceName());
            text.setText("" + value);//$NON-NLS-1$
        }

    }

    /* (non-Javadoc)
     * Method declared on FieldEditor.
     */
    protected void doLoadDefault() {
        Text text = getTextControl();
        if (text != null) {
            float value = getPreferenceStore().getDefaultFloat(getPreferenceName());
            text.setText("" + value);//$NON-NLS-1$
        }
        valueChanged();
    }

    /* (non-Javadoc)
     * Method declared on FieldEditor.
     */
    protected void doStore() {
        Text text = getTextControl();
        if (text != null) {
            Float i = new Float(text.getText());
            getPreferenceStore().setValue(getPreferenceName(), i.floatValue());
        }
    }

    /**
     * Returns this field editor's current value as an integer.
     *
     * @return the value
     * @exception NumberFormatException if the <code>String</code> does not
     *   contain a parsable integer
     */
    public float getFloatValue() throws NumberFormatException {
        return new Float(getStringValue()).floatValue();
    }

}
