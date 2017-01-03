package com.sparty02.rx.file.watcher;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.common.jimfs.WatchServiceConfiguration;

import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class AppTest {

	private FileSystem fileSystem;

	@Before
	public void init() {
		Configuration config = Configuration.forCurrentPlatform().toBuilder()
				.setWatchServiceConfiguration(WatchServiceConfiguration.polling(10, MILLISECONDS)).build();
		fileSystem = Jimfs.newFileSystem(config);
	}

	@After
	public void setup() throws IOException {
		fileSystem.close();
	}

	public static void main(String[] args) {
		FileSystem fileSystem = FileSystems.getDefault();
		ObservableFileWatcher fileWatcher = new ObservableFileWatcher(fileSystem);
		Path watchPath = fileSystem.getPath("/tmp");
		fileWatcher.watch(watchPath)
				.subscribe(event -> System.out.println(String.format("%s: %s", event.getKind(), event.getPath())));
	}

	@Test
	public void fileWriteShouldTriggerWatch() {
		ObservableFileWatcher fileWatcher = new ObservableFileWatcher(fileSystem);

		Path testDir1 = mkDirP(fileSystem, "/content1");

		TestSubscriber<PathEvent> testSubscriber1 = new TestSubscriber<>();
		Path testFile1 = touch(fileSystem, testDir1.resolve("text.txt"));

		fileWatcher.watch(testDir1).subscribeOn(Schedulers.io()).subscribe(testSubscriber1);

		Observable.interval(100, 10, MILLISECONDS).doOnEach(tick -> {
			writeToFile(testFile1, String.format("hello world %s", tick.getValue()));
		}).take(5).toBlocking().subscribe();

		testSubscriber1.getOnErrorEvents().forEach(t -> t.printStackTrace());

		assertThat(testSubscriber1.awaitValueCount(5, 500, MILLISECONDS)).isTrue();
		testSubscriber1.assertValueCount(5);
	}

	@Test
	public void shouldBeAbleToWatchSeparateDirsFromSameWatcher() {
		ObservableFileWatcher fileWatcher = new ObservableFileWatcher(fileSystem);

		/*
		 * Round 1
		 */

		Path testDir1 = mkDirP(fileSystem, "/content1");

		TestSubscriber<PathEvent> testSubscriber1 = new TestSubscriber<>();
		Path testFile1 = touch(fileSystem, testDir1.resolve("text.txt"));

		fileWatcher.watch(testDir1).subscribeOn(Schedulers.io()).subscribe(testSubscriber1);

		Observable.interval(100, 10, MILLISECONDS).doOnEach(tick -> {
			writeToFile(testFile1, String.format("hello world %s", tick.getValue()));
		}).take(5).toBlocking().subscribe();

		testSubscriber1.getOnErrorEvents().forEach(t -> t.printStackTrace());

		assertThat(testSubscriber1.awaitValueCount(5, 500, MILLISECONDS)).isTrue();
		testSubscriber1.assertValueCount(5);

		/*
		 * Round 2
		 */

		Path testDir2 = mkDirP(fileSystem, "/content2");
		Path testFile2 = touch(fileSystem, testDir2.resolve("text.txt"));

		TestSubscriber<PathEvent> testSubscriber2 = new TestSubscriber<>();

		fileWatcher.watch(testDir2).subscribeOn(Schedulers.io()).subscribe(testSubscriber2);

		Observable.interval(500, 10, MILLISECONDS).doOnEach(tick -> {
			writeToFile(testFile2, String.format("hello world %s", tick.getValue()));
		}).take(5).subscribe();

		// assertThat(testSubscriber2.awaitValueCount(5, 10000,
		// MILLISECONDS)).isTrue();
		// testSubscriber2.assertValueCount(5);

		// testSubscriber.assertValues(new PathEvent(testFile2, ENTRY_MODIFY),
		// new PathEvent(testFile2, ENTRY_MODIFY),
		// new PathEvent(testFile2, ENTRY_MODIFY), new PathEvent(testFile2,
		// ENTRY_MODIFY),
		// new PathEvent(testFile2, ENTRY_MODIFY));
	}

	@Test
	public void test3() {
		TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();
		Observable.range(1, 5).switchMap(number -> {
			return Observable.just(number);
		}).doOnEach(number -> System.out.println(number)).subscribe(testSubscriber);
	}

	@Test
	public void test4() {
		mkDirP(fileSystem, "/content");

		TestSubscriber<List<Path>> testSubscriber = new TestSubscriber<>();

		Observable.interval(100, MILLISECONDS).scan(new ArrayList<Path>(), (list, item) -> {
			Path path = fileSystem.getPath("/content", item.toString());
			mkDirP(fileSystem, path);
			list.add(path);
			return list;
		}).doOnEach(list -> {
			System.out.println(list.getValue());
		}).take(5).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.getOnErrorEvents().forEach(t -> t.printStackTrace());
	}

	private Path touch(FileSystem fileSystem, Path filePath) {
		try {
			Files.createFile(filePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return filePath;
	}

	private Path mkDirP(FileSystem fileSystem, String path) {
		Path dirPath = fileSystem.getPath(path);
		return mkDirP(fileSystem, dirPath);
	}

	private Path mkDirP(FileSystem fileSystem, Path path) {
		try {
			Files.createDirectories(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return path;
	}

	private void writeToFile(Path file, String text) {
		try {
			Files.write(file, text.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
