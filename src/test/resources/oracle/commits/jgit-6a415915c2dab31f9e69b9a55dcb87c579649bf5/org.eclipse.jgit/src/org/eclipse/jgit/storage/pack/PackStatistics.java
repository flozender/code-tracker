/*
 * Copyright (C) 2015, Google Inc.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.jgit.storage.pack;

import static org.eclipse.jgit.lib.Constants.OBJ_BLOB;
import static org.eclipse.jgit.lib.Constants.OBJ_COMMIT;
import static org.eclipse.jgit.lib.Constants.OBJ_TAG;
import static org.eclipse.jgit.lib.Constants.OBJ_TREE;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.pack.CachedPack;
import org.eclipse.jgit.lib.ObjectId;

/**
 * Statistics about {@link org.eclipse.jgit.internal.storage.pack.PackWriter}
 * pack creation.
 *
 * @since 4.1
 */
public class PackStatistics {
	/**
	 * Statistics about a single type of object (commits, tags, trees and
	 * blobs).
	 */
	public static class ObjectType {
		/**
		 * POJO for accumulating the ObjectType statistics.
		 */
		public static class Accumulator {
			/** Count of objects of this type. */
			public long cntObjects;

			/** Count of deltas of this type. */
			public long cntDeltas;

			/** Count of reused objects of this type. */
			public long reusedObjects;

			/** Count of reused deltas of this type. */
			public long reusedDeltas;

			/** Count of bytes for all objects of this type. */
			public long bytes;

			/** Count of delta bytes for objects of this type. */
			public long deltaBytes;
		}

		private ObjectType.Accumulator objectType;

		/**
		 * Creates a new {@link ObjectType} object from the accumulator.
		 *
		 * @param accumulator
		 *            the accumulator of the statistics
		 */
		public ObjectType(ObjectType.Accumulator accumulator) {
			objectType = accumulator;
		}

		/**
		 * @return total number of objects output. This total includes the value
		 *         of {@link #getDeltas()}.
		 */
		public long getObjects() {
			return objectType.cntObjects;
		}

		/**
		 * @return total number of deltas output. This may be lower than the
		 *         actual number of deltas if a cached pack was reused.
		 */
		public long getDeltas() {
			return objectType.cntDeltas;
		}

		/**
		 * @return number of objects whose existing representation was reused in
		 *         the output. This count includes {@link #getReusedDeltas()}.
		 */
		public long getReusedObjects() {
			return objectType.reusedObjects;
		}

		/**
		 * @return number of deltas whose existing representation was reused in
		 *         the output, as their base object was also output or was
		 *         assumed present for a thin pack. This may be lower than the
		 *         actual number of reused deltas if a cached pack was reused.
		 */
		public long getReusedDeltas() {
			return objectType.reusedDeltas;
		}

		/**
		 * @return total number of bytes written. This size includes the object
		 *         headers as well as the compressed data. This size also
		 *         includes all of {@link #getDeltaBytes()}.
		 */
		public long getBytes() {
			return objectType.bytes;
		}

		/**
		 * @return number of delta bytes written. This size includes the object
		 *         headers for the delta objects.
		 */
		public long getDeltaBytes() {
			return objectType.deltaBytes;
		}
	}

	/**
	 * POJO for accumulating the statistics.
	 */
	public static class Accumulator {
		/** The set of objects to be included in the pack. */
		public Set<ObjectId> interestingObjects;

		/** The set of objects to be excluded from the pack. */
		public Set<ObjectId> uninterestingObjects;

		/** The set of shallow commits on the client. */
		public Set<ObjectId> clientShallowCommits;

		/** The collection of reused packs in the upload. */
		public List<CachedPack> reusedPacks;

		/** If a shallow pack, the depth in commits. */
		public int depth;

		/**
		 * The count of objects in the pack that went through the delta search
		 * process in order to find a potential delta base.
		 */
		public int deltaSearchNonEdgeObjects;

		/**
		 * The count of objects in the pack that went through delta base search
		 * and found a suitable base. This is a subset of
		 * deltaSearchNonEdgeObjects.
		 */
		public int deltasFound;

		/** The total count of objects in the pack. */
		public long totalObjects;

		/**
		 * The count of objects that needed to be discovered through an object
		 * walk because they were not found in bitmap indices.
		 */
		public long bitmapIndexMisses;

		/** The total count of deltas output. */
		public long totalDeltas;

		/** The count of reused objects in the pack. */
		public long reusedObjects;

		/** The count of reused deltas in the pack. */
		public long reusedDeltas;

		/** The count of total bytes in the pack. */
		public long totalBytes;

		/** The size of the thin pack in bytes, if a thin pack was generated. */
		public long thinPackBytes;

		/** Time in ms spent counting the objects that will go into the pack. */
		public long timeCounting;

		/** Time in ms spent searching for objects to reuse. */
		public long timeSearchingForReuse;

		/** Time in ms spent searching for sizes of objects. */
		public long timeSearchingForSizes;

		/** Time in ms spent compressing the pack. */
		public long timeCompressing;

		/** Time in ms spent writing the pack. */
		public long timeWriting;

		/**
		 * Statistics about each object type in the pack (commits, tags, trees
		 * and blobs.)
		 */
		public ObjectType.Accumulator[] objectTypes;

		{
			objectTypes = new ObjectType.Accumulator[5];
			objectTypes[OBJ_COMMIT] = new ObjectType.Accumulator();
			objectTypes[OBJ_TREE] = new ObjectType.Accumulator();
			objectTypes[OBJ_BLOB] = new ObjectType.Accumulator();
			objectTypes[OBJ_TAG] = new ObjectType.Accumulator();
		}
	}

	private Accumulator statistics;

	/**
	 * Creates a new {@link PackStatistics} object from the accumulator.
	 *
	 * @param accumulator
	 *            the accumulator of the statistics
	 */
	public PackStatistics(Accumulator accumulator) {
		// Note: PackStatistics directly serves up the collections in the
		// accumulator.
		statistics = accumulator;
	}

	/**
	 * @return unmodifiable collection of objects to be included in the pack.
	 *         May be {@code null} if the pack was hand-crafted in a unit test.
	 */
	public Set<ObjectId> getInterestingObjects() {
		return statistics.interestingObjects;
	}

	/**
	 * @return unmodifiable collection of objects that should be excluded from
	 *         the pack, as the peer that will receive the pack already has
	 *         these objects.
	 */
	public Set<ObjectId> getUninterestingObjects() {
		return statistics.uninterestingObjects;
	}

	/**
	 * @return unmodifiable collection of objects that were shallow commits on
	 *         the client.
	 */
	public Set<ObjectId> getClientShallowCommits() {
		return statistics.clientShallowCommits;
	}

	/**
	 * @return unmodifiable list of the cached packs that were reused in the
	 *         output, if any were selected for reuse.
	 */
	public List<CachedPack> getReusedPacks() {
		return statistics.reusedPacks;
	}

	/**
	 * @return number of objects in the output pack that went through the delta
	 *         search process in order to find a potential delta base.
	 */
	public int getDeltaSearchNonEdgeObjects() {
		return statistics.deltaSearchNonEdgeObjects;
	}

	/**
	 * @return number of objects in the output pack that went through delta base
	 *         search and found a suitable base. This is a subset of
	 *         {@link #getDeltaSearchNonEdgeObjects()}.
	 */
	public int getDeltasFound() {
		return statistics.deltasFound;
	}

	/**
	 * @return total number of objects output. This total includes the value of
	 *         {@link #getTotalDeltas()}.
	 */
	public long getTotalObjects() {
		return statistics.totalObjects;
	}

	/**
	 * @return the count of objects that needed to be discovered through an
	 *         object walk because they were not found in bitmap indices.
	 *         Returns -1 if no bitmap indices were found.
	 */
	public long getBitmapIndexMisses() {
		return statistics.bitmapIndexMisses;
	}

	/**
	 * @return total number of deltas output. This may be lower than the actual
	 *         number of deltas if a cached pack was reused.
	 */
	public long getTotalDeltas() {
		return statistics.totalDeltas;
	}

	/**
	 * @return number of objects whose existing representation was reused in the
	 *         output. This count includes {@link #getReusedDeltas()}.
	 */
	public long getReusedObjects() {
		return statistics.reusedObjects;
	}

	/**
	 * @return number of deltas whose existing representation was reused in the
	 *         output, as their base object was also output or was assumed
	 *         present for a thin pack. This may be lower than the actual number
	 *         of reused deltas if a cached pack was reused.
	 */
	public long getReusedDeltas() {
		return statistics.reusedDeltas;
	}

	/**
	 * @return total number of bytes written. This size includes the pack
	 *         header, trailer, thin pack, and reused cached pack(s).
	 */
	public long getTotalBytes() {
		return statistics.totalBytes;
	}

	/**
	 * @return size of the thin pack in bytes, if a thin pack was generated. A
	 *         thin pack is created when the client already has objects and some
	 *         deltas are created against those objects, or if a cached pack is
	 *         being used and some deltas will reference objects in the cached
	 *         pack. This size does not include the pack header or trailer.
	 */
	public long getThinPackBytes() {
		return statistics.thinPackBytes;
	}

	/**
	 * @param typeCode
	 *            object type code, e.g. OBJ_COMMIT or OBJ_TREE.
	 * @return information about this type of object in the pack.
	 */
	public ObjectType byObjectType(int typeCode) {
		return new ObjectType(statistics.objectTypes[typeCode]);
	}

	/** @return true if the resulting pack file was a shallow pack. */
	public boolean isShallow() {
		return statistics.depth > 0;
	}

	/** @return depth (in commits) the pack includes if shallow. */
	public int getDepth() {
		return statistics.depth;
	}

	/**
	 * @return time in milliseconds spent enumerating the objects that need to
	 *         be included in the output. This time includes any restarts that
	 *         occur when a cached pack is selected for reuse.
	 */
	public long getTimeCounting() {
		return statistics.timeCounting;
	}

	/**
	 * @return time in milliseconds spent matching existing representations
	 *         against objects that will be transmitted, or that the client can
	 *         be assumed to already have.
	 */
	public long getTimeSearchingForReuse() {
		return statistics.timeSearchingForReuse;
	}

	/**
	 * @return time in milliseconds spent finding the sizes of all objects that
	 *         will enter the delta compression search window. The sizes need to
	 *         be known to better match similar objects together and improve
	 *         delta compression ratios.
	 */
	public long getTimeSearchingForSizes() {
		return statistics.timeSearchingForSizes;
	}

	/**
	 * @return time in milliseconds spent on delta compression. This is observed
	 *         wall-clock time and does not accurately track CPU time used when
	 *         multiple threads were used to perform the delta compression.
	 */
	public long getTimeCompressing() {
		return statistics.timeCompressing;
	}

	/**
	 * @return time in milliseconds spent writing the pack output, from start of
	 *         header until end of trailer. The transfer speed can be
	 *         approximated by dividing {@link #getTotalBytes()} by this value.
	 */
	public long getTimeWriting() {
		return statistics.timeWriting;
	}

	/** @return total time spent processing this pack. */
	public long getTimeTotal() {
		return statistics.timeCounting + statistics.timeSearchingForReuse
				+ statistics.timeSearchingForSizes + statistics.timeCompressing
				+ statistics.timeWriting;
	}

	/**
	 * @return get the average output speed in terms of bytes-per-second.
	 *         {@code getTotalBytes() / (getTimeWriting() / 1000.0)}.
	 */
	public double getTransferRate() {
		return getTotalBytes() / (getTimeWriting() / 1000.0);
	}

	/** @return formatted message string for display to clients. */
	public String getMessage() {
		return MessageFormat.format(JGitText.get().packWriterStatistics,
				Long.valueOf(statistics.totalObjects),
				Long.valueOf(statistics.totalDeltas),
				Long.valueOf(statistics.reusedObjects),
				Long.valueOf(statistics.reusedDeltas));
	}

	/** @return a map containing ObjectType statistics. */
	public Map<Integer, ObjectType> getObjectTypes() {
		HashMap<Integer, ObjectType> map = new HashMap<>();
		map.put(Integer.valueOf(OBJ_BLOB), new ObjectType(
				statistics.objectTypes[OBJ_BLOB]));
		map.put(Integer.valueOf(OBJ_COMMIT), new ObjectType(
				statistics.objectTypes[OBJ_COMMIT]));
		map.put(Integer.valueOf(OBJ_TAG), new ObjectType(
				statistics.objectTypes[OBJ_TAG]));
		map.put(Integer.valueOf(OBJ_TREE), new ObjectType(
				statistics.objectTypes[OBJ_TREE]));
		return map;
	}
}
