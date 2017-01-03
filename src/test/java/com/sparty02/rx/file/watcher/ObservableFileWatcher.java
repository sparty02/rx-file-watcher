package com.sparty02.rx.file.watcher;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

import rx.Observable;
import rx.subscriptions.Subscriptions;

public class ObservableFileWatcher {

	private final WatchService watchService;
	private Observable<PathEvent> watchObservable;

	public ObservableFileWatcher(FileSystem fileSystem) {
		try {
			this.watchService = fileSystem.newWatchService();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.watchObservable = create();
	}

	public Observable<PathEvent> watch(Path watchPath) {
		if (!Files.isDirectory(watchPath)) {
			throw new IllegalArgumentException(String.format("Path (%s) must be a directory", watchPath));
		}

		return Observable.<PathEvent>create(subscriber -> {
			try {
				WatchKey key = watchPath.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
				subscriber.add(Subscriptions.create(() -> key.cancel()));

				watchObservable.filter(pathEvent -> pathEvent.getPath().getParent().equals(watchPath))
						.subscribe(pathEvent -> subscriber.onNext(pathEvent));
			} catch (IOException e) {
				subscriber.onError(e);
			}
		});
	}

	private Observable<PathEvent> create() {
		Observable<PathEvent> watchPathObservable = Observable.create(subscriber -> {
			try {
				subscriber.add(Subscriptions.create(() -> {
					try {
						watchService.close();
					} catch (IOException e) {
						subscriber.onError(e);
					}
				}));

				while (true) {
					WatchKey key = watchService.take();
					Path dir = (Path) key.watchable();

					List<WatchEvent<?>> events = key.pollEvents();
					for (WatchEvent<?> event : events) {
						@SuppressWarnings("unchecked")
						WatchEvent<Path> pathWatchEvent = (WatchEvent<Path>) event;
						Path fullPath = dir.resolve(pathWatchEvent.context());

						subscriber.onNext(new PathEvent(fullPath, pathWatchEvent.kind()));
					}
					key.reset();
				}
			} catch (InterruptedException e) {
				subscriber.onError(e);
			}
		});

		return watchPathObservable.share();// .subscribeOn(Schedulers.io()).publish().refCount();
	}

}
