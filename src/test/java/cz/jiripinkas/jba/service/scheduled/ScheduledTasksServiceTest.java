package cz.jiripinkas.jba.service.scheduled;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ScheduledTasksServiceTest {

	private ScheduledTasksService scheduledTasksService;

	@Before
	public void setup() {
		scheduledTasksService = new ScheduledTasksService();
	}

	@Test
	public void shouldReturn_01_2015() throws ParseException {
		Date firstDayOfYear = new Date(new SimpleDateFormat("dd.MM.yyyy").parse("01.01.2015").getTime());
		int[] weekAndYear = scheduledTasksService.getCurrentWeekAndYear(firstDayOfYear);
		Assert.assertEquals(1, weekAndYear[0]);
		Assert.assertEquals(2015, weekAndYear[1]);
	}

	@Test
	public void shouldReturn_02_2015() throws ParseException {
		Date firstDayOfYear = new Date(new SimpleDateFormat("dd.MM.yyyy").parse("08.01.2015").getTime());
		int[] weekAndYear = scheduledTasksService.getCurrentWeekAndYear(firstDayOfYear);
		Assert.assertEquals(2, weekAndYear[0]);
		Assert.assertEquals(2015, weekAndYear[1]);
	}

	@Test
	public void shouldReturn_01_2016() throws ParseException {
		Date firstDayOfYear = new Date(new SimpleDateFormat("dd.MM.yyyy").parse("01.01.2016").getTime());
		int[] weekAndYear = scheduledTasksService.getCurrentWeekAndYear(firstDayOfYear);
		Assert.assertEquals(1, weekAndYear[0]);
		Assert.assertEquals(2016, weekAndYear[1]);
	}

	@Test
	public void testReindexTimeoutPassed() {
		{
			Calendar calendar = new GregorianCalendar();
			calendar.add(Calendar.DATE, -2);
			Assert.assertTrue(scheduledTasksService.reindexTimeoutPassed(calendar.getTime()));
		}
		{
			Calendar calendar = new GregorianCalendar();
			calendar.add(Calendar.HOUR_OF_DAY, -1);
			Assert.assertFalse(scheduledTasksService.reindexTimeoutPassed(calendar.getTime()));
		}
		Assert.assertTrue(scheduledTasksService.reindexTimeoutPassed(null));
	}

}
