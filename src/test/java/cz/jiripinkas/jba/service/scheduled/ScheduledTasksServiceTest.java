package cz.jiripinkas.jba.service.scheduled;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;

class ScheduledTasksServiceTest {

	private ScheduledTasksService scheduledTasksService;

	@BeforeEach
	public void setup() {
		scheduledTasksService = new ScheduledTasksService();
	}

	@Test
	void shouldReturn_01_2015() throws ParseException {
		Date firstDayOfYear = new Date(new SimpleDateFormat("dd.MM.yyyy").parse("01.01.2015").getTime());
		int[] weekAndYear = scheduledTasksService.getCurrentWeekAndYear(firstDayOfYear);
		assertEquals(1, weekAndYear[0]);
		assertEquals(2015, weekAndYear[1]);
	}

	@Test
	void shouldReturn_02_2015() throws ParseException {
		Date firstDayOfYear = new Date(new SimpleDateFormat("dd.MM.yyyy").parse("08.01.2015").getTime());
		int[] weekAndYear = scheduledTasksService.getCurrentWeekAndYear(firstDayOfYear);
		assertEquals(2, weekAndYear[0]);
		assertEquals(2015, weekAndYear[1]);
	}

	@Test
	void shouldReturn_01_2016() throws ParseException {
		Date firstDayOfYear = new Date(new SimpleDateFormat("dd.MM.yyyy").parse("01.01.2016").getTime());
		int[] weekAndYear = scheduledTasksService.getCurrentWeekAndYear(firstDayOfYear);
		assertEquals(1, weekAndYear[0]);
		assertEquals(2016, weekAndYear[1]);
	}

	@Test
	void testReindexTimeoutPassed() {
		{
			Calendar calendar = new GregorianCalendar();
			calendar.add(Calendar.DATE, -2);
			assertTrue(scheduledTasksService.reindexTimeoutPassed(calendar.getTime()));
		}
		{
			Calendar calendar = new GregorianCalendar();
			calendar.add(Calendar.HOUR_OF_DAY, -1);
			assertFalse(scheduledTasksService.reindexTimeoutPassed(calendar.getTime()));
		}
		assertTrue(scheduledTasksService.reindexTimeoutPassed(null));
	}

}
