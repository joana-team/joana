
package dummy;

import android.app.DatePickerDialog.OnDateSetListener;
import android.widget.DatePicker;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.widget.TimePicker;

public class DateTimeListener implements OnDateSetListener, OnTimeSetListener {
    public DateTimeListener() { }

    public void   onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) { }
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) { }
}
