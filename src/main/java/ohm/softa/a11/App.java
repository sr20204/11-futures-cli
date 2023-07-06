package ohm.softa.a11;

import ohm.softa.a11.openmensa.OpenMensaAPI;
import ohm.softa.a11.openmensa.OpenMensaAPIService;
import ohm.softa.a11.openmensa.model.Canteen;
import ohm.softa.a11.openmensa.model.PageInfo;
import retrofit2.Response;

import javax.naming.CannotProceedException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import org.apache.commons.lang3.time.StopWatch;

/**
 * @author Peter Kurfer
 * Created on 12/16/17.
 */
public class App {
	private static final String OPEN_MENSA_DATE_FORMAT = "yyyy-MM-dd";

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat(OPEN_MENSA_DATE_FORMAT, Locale.getDefault());
	private static final Scanner inputScanner = new Scanner(System.in);
	private static final OpenMensaAPI openMensaAPI = OpenMensaAPIService.getInstance().getOpenMensaAPI();
	private static final Calendar currentDate = Calendar.getInstance();
	private static int currentCanteenId = -1;

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		MenuSelection selection;
		/* loop while true to get back to the menu every time an action was performed */
		do {
			selection = menu();
			switch (selection) {
				case SHOW_CANTEENS:
					printCanteens();
					break;
				case SET_CANTEEN:
					readCanteen();
					break;
				case SHOW_MEALS:
					printMeals();
					break;
				case SET_DATE:
					readDate();
					break;
				case QUIT:
					System.exit(0);

			}
		} while (true);
	}

	private static void printCanteens() throws ExecutionException, InterruptedException {
		System.out.print("Fetching canteens [");
		/* TODO fetch all canteens and print them to STDOUT
		 * at first get a page without an index to be able to extract the required pagination information
		 * afterwards you can iterate the remaining pages
		 * keep in mind that you should await the process as the user has to select canteen with a specific id */
		/*CompletableFuture<Response<List<Canteen>>> firstPage = openMensaAPI.getCanteens();
		PageInfo pageInfo = null;
		try {
			pageInfo = PageInfo.extractFromResponse(firstPage.get());
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		System.out.println("");
		int i = pageInfo.getCurrentPageIndex();

		List<Canteen> canteens = new LinkedList<>();
		while(i <= pageInfo.getTotalCountOfPages()){
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			CompletableFuture<List<Canteen>> pages = openMensaAPI.getCanteens(i);
			try {
				//System.out.println(pages.get());
				canteens.addAll(pages.get());
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
			stopWatch.stop();
			System.out.println(stopWatch.getTime());
			i++;
		}
		for(Canteen c : canteens){
			System.out.println(c);
		}*/

		List<Canteen> canteens1 = new LinkedList<>();
		openMensaAPI.getCanteens()
			.thenApplyAsync(canteens2 -> {
				List<Canteen> can = canteens2.body();
				PageInfo pageInfo = PageInfo.extractFromResponse(canteens2);
				for(int i = pageInfo.getCurrentPageIndex()+1; i <pageInfo.getTotalCountOfPages();i++ )
				{
					try {
						can.addAll(openMensaAPI.getCanteens(i).get());
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					} catch (ExecutionException e) {
						throw new RuntimeException(e);
					}
				}
				return can;
			})
			.thenAcceptAsync(canteens -> {
				System.out.println(canteens);
			})
			.exceptionally(throwable -> {
				System.out.println("Something went wrong");
				if(throwable.getClass() == UnknownHostException.class)
				{
					System.out.println("check your Internet cant connect to host");
				}
				return null;
			})

		.get();

	}

	private static void printMeals() throws ExecutionException, InterruptedException {
		/* TODO fetch all meals for the currently selected canteen
		 * to avoid errors retrieve at first the state of the canteen and check if the canteen is opened at the selected day
		 * don't forget to check if a canteen was selected previously! */

		if(currentCanteenId < 0){
			System.out.println("Set CanteenId");
			return;
		}
		openMensaAPI.getCanteenState(currentCanteenId,currentDate.getTime().toString())
				.thenApply(make -> {
					if(make.isClosed())
					{
						System.out.println("Canteen clodes");
						return false;

					}
					try {
						openMensaAPI.getMeals(currentCanteenId,currentDate.getTime().toString())
							.thenApply(meals -> {
								return meals;
							})
							.thenAccept(meals -> {
								System.out.println(meals);
							}).get();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					} catch (ExecutionException e) {
						throw new RuntimeException(e);
					}
					return true;
				}).get();

	}

	/**
	 * Utility method to select a canteen
	 */
	private static void readCanteen() {
		/* typical input reading pattern */
		boolean readCanteenId = false;
		do {
			try {
				System.out.println("Enter canteen id:");
				currentCanteenId = inputScanner.nextInt();
				readCanteenId = true;
			} catch (Exception e) {
				System.out.println("Sorry could not read the canteen id");
			}
		} while (!readCanteenId);
	}

	/**
	 * Utility method to read a date and update the calendar
	 */
	private static void readDate() {
		/* typical input reading pattern */
		boolean readDate = false;
		do {
			try {
				System.out.println("Pleae enter date in the format yyyy-mm-dd:");
				Date d = dateFormat.parse(inputScanner.next());
				currentDate.setTime(d);
				readDate = true;
			} catch (ParseException p) {
				System.out.println("Sorry, the entered date could not be parsed.");
			}
		} while (!readDate);

	}

	/**
	 * Utility method to print menu and read the user selection
	 *
	 * @return user selection as MenuSelection
	 */
	private static MenuSelection menu() {
		IntStream.range(0, 20).forEach(i -> System.out.print("#"));
		System.out.println();
		System.out.println("1) Show canteens");
		System.out.println("2) Set canteen");
		System.out.println("3) Show meals");
		System.out.println("4) Set date");
		System.out.println("5) Quit");
		IntStream.range(0, 20).forEach(i -> System.out.print("#"));
		System.out.println();

		switch (inputScanner.nextInt()) {
			case 1:
				return MenuSelection.SHOW_CANTEENS;
			case 2:
				return MenuSelection.SET_CANTEEN;
			case 3:
				return MenuSelection.SHOW_MEALS;
			case 4:
				return MenuSelection.SET_DATE;
			default:
				return MenuSelection.QUIT;
		}
	}
}
