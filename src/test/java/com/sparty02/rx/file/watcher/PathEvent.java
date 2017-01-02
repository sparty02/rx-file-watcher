package com.sparty02.rx.file.watcher;

import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;

import com.google.common.base.Objects;

public class PathEvent {

	private final Path path;
	private final Kind<Path> kind;

	public PathEvent(Path path, Kind<Path> kind) {
		this.path = path;
		this.kind = kind;
	}

	public Path getPath() {
		return path;
	}

	public Kind<Path> getKind() {
		return kind;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(path, kind);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}

		PathEvent other = (PathEvent) obj;
		return Objects.equal(path, other.path) && Objects.equal(kind, other.kind);
	}

}
