package harvester.client.util;


/**
 * just a convenient way of holding a time
 */
public class ViewTime {
	private int hour;
	private int minute;
	
	public ViewTime(int hour, int minute)
	{
		this.hour = hour;
		this.minute = minute;
	}
	
	public ViewTime()
	{
		
	}
	
	public int getHour() {
		return hour;
	}
	public void setHour(int hour) {
		this.hour = hour;
	}
	public int getMinute() {
		return minute;
	}
	public void setMinute(int minute) {
		this.minute = minute;
	}
}
