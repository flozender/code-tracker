/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 *  Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 *  indicated by the @author tags or express copyright attribution
 *  statements applied by the authors.  All third-party contributions are
 *  distributed under license by Red Hat, Inc.
 *
 *  This copyrighted material is made available to anyone wishing to use, modify,
 *  copy, or redistribute it subject to the terms and conditions of the GNU
 *  Lesser General Public License, as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this distribution; if not, write to:
 *  Free Software Foundation, Inc.
 *  51 Franklin Street, Fifth Floor
 *  Boston, MA  02110-1301  USA
 */

package org.apache.solr.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeSource;

/**
 * SynonymFilter handles multi-token synonyms with variable position increment offsets.
 * <p/>
 * The matched tokens from the input stream may be optionally passed through (includeOrig=true)
 * or discarded.  If the original tokens are included, the position increments may be modified
 * to retain absolute positions after merging with the synonym tokenstream.
 * <p/>
 * Generated synonyms will start at the same position as the first matched source token.
 *
 * @version $Id: SynonymFilter.java 991055 2010-08-31 01:40:19Z rmuir $
 */
public final class SynonymFilter extends TokenFilter {

	private final SynonymMap map;  // Map<String, SynonymMap>
	private Iterator<AttributeSource> replacement;  // iterator over generated tokens

	public SynonymFilter(TokenStream in, SynonymMap map) {
		super( in );
		this.map = map;
		// just ensuring these attributes exist...
		addAttribute( TermAttribute.class );
		addAttribute( PositionIncrementAttribute.class );
		addAttribute( OffsetAttribute.class );
		addAttribute( TypeAttribute.class );
	}


	/*
	   * Need to worry about multiple scenarios:
	   *  - need to go for the longest match
	   *    a b => foo      #shouldn't match if "a b" is followed by "c d"
	   *    a b c d => bar
	   *  - need to backtrack - retry matches for tokens already read
	   *     a b c d => foo
	   *       b c => bar
	   *     If the input stream is "a b c x", one will consume "a b c d"
	   *     trying to match the first rule... all but "a" should be
	   *     pushed back so a match may be made on "b c".
	   *  - don't try and match generated tokens (thus need separate queue)
	   *    matching is not recursive.
	   *  - handle optional generation of original tokens in all these cases,
	   *    merging token streams to preserve token positions.
	   *  - preserve original positionIncrement of first matched token
	   */

	@Override
	public boolean incrementToken() throws IOException {
		while ( true ) {
			// if there are any generated tokens, return them... don't try any
			// matches against them, as we specifically don't want recursion.
			if ( replacement != null && replacement.hasNext() ) {
				copy( this, replacement.next() );
				return true;
			}

			// common case fast-path of first token not matching anything
			AttributeSource firstTok = nextTok();
			if ( firstTok == null ) {
				return false;
			}
			TermAttribute termAtt = firstTok.addAttribute( TermAttribute.class );
			SynonymMap result = map.submap != null ? map.submap
					.get( termAtt.termBuffer(), 0, termAtt.termLength() ) : null;
			if ( result == null ) {
				copy( this, firstTok );
				return true;
			}

			// fast-path failed, clone ourselves if needed
			if ( firstTok == this ) {
				firstTok = cloneAttributes();
			}
			// OK, we matched a token, so find the longest match.

			matched = new LinkedList<AttributeSource>();

			result = match( result );

			if ( result == null ) {
				// no match, simply return the first token read.
				copy( this, firstTok );
				return true;
			}

			// reuse, or create new one each time?
			ArrayList<AttributeSource> generated = new ArrayList<AttributeSource>( result.synonyms.length + matched.size() + 1 );

			//
			// there was a match... let's generate the new tokens, merging
			// in the matched tokens (position increments need adjusting)
			//
			AttributeSource lastTok = matched.isEmpty() ? firstTok : matched.getLast();
			boolean includeOrig = result.includeOrig();

			AttributeSource origTok = includeOrig ? firstTok : null;
			PositionIncrementAttribute firstPosIncAtt = firstTok.addAttribute( PositionIncrementAttribute.class );
			int origPos = firstPosIncAtt.getPositionIncrement();  // position of origTok in the original stream
			int repPos = 0; // curr position in replacement token stream
			int pos = 0;  // current position in merged token stream

			for ( int i = 0; i < result.synonyms.length; i++ ) {
				Token repTok = result.synonyms[i];
				AttributeSource newTok = firstTok.cloneAttributes();
				TermAttribute newTermAtt = newTok.addAttribute( TermAttribute.class );
				OffsetAttribute newOffsetAtt = newTok.addAttribute( OffsetAttribute.class );
				PositionIncrementAttribute newPosIncAtt = newTok.addAttribute( PositionIncrementAttribute.class );

				OffsetAttribute lastOffsetAtt = lastTok.addAttribute( OffsetAttribute.class );

				newOffsetAtt.setOffset( newOffsetAtt.startOffset(), lastOffsetAtt.endOffset() );
				newTermAtt.setTermBuffer( repTok.termBuffer(), 0, repTok.termLength() );
				repPos += repTok.getPositionIncrement();
				if ( i == 0 ) {
					repPos = origPos;
				}  // make position of first token equal to original

				// if necessary, insert original tokens and adjust position increment
				while ( origTok != null && origPos <= repPos ) {
					PositionIncrementAttribute origPosInc = origTok.addAttribute( PositionIncrementAttribute.class );
					origPosInc.setPositionIncrement( origPos - pos );
					generated.add( origTok );
					pos += origPosInc.getPositionIncrement();
					origTok = matched.isEmpty() ? null : matched.removeFirst();
					if ( origTok != null ) {
						origPosInc = origTok.addAttribute( PositionIncrementAttribute.class );
						origPos += origPosInc.getPositionIncrement();
					}
				}

				newPosIncAtt.setPositionIncrement( repPos - pos );
				generated.add( newTok );
				pos += newPosIncAtt.getPositionIncrement();
			}

			// finish up any leftover original tokens
			while ( origTok != null ) {
				PositionIncrementAttribute origPosInc = origTok.addAttribute( PositionIncrementAttribute.class );
				origPosInc.setPositionIncrement( origPos - pos );
				generated.add( origTok );
				pos += origPosInc.getPositionIncrement();
				origTok = matched.isEmpty() ? null : matched.removeFirst();
				if ( origTok != null ) {
					origPosInc = origTok.addAttribute( PositionIncrementAttribute.class );
					origPos += origPosInc.getPositionIncrement();
				}
			}

			// what if we replaced a longer sequence with a shorter one?
			// a/0 b/5 =>  foo/0
			// should I re-create the gap on the next buffered token?

			replacement = generated.iterator();
			// Now return to the top of the loop to read and return the first
			// generated token.. The reason this is done is that we may have generated
			// nothing at all, and may need to continue with more matching logic.
		}
	}


	//
	// Defer creation of the buffer until the first time it is used to
	// optimize short fields with no matches.
	//
	private LinkedList<AttributeSource> buffer;
	private LinkedList<AttributeSource> matched;

	private AttributeSource nextTok() throws IOException {
		if ( buffer != null && !buffer.isEmpty() ) {
			return buffer.removeFirst();
		}
		else {
			if ( input.incrementToken() ) {
				return this;
			}
			else {
				return null;
			}
		}
	}

	private void pushTok(AttributeSource t) {
		if ( buffer == null ) {
			buffer = new LinkedList<AttributeSource>();
		}
		buffer.addFirst( t );
	}

	private SynonymMap match(SynonymMap map) throws IOException {
		SynonymMap result = null;

		if ( map.submap != null ) {
			AttributeSource tok = nextTok();
			if ( tok != null ) {
				// clone ourselves.
				if ( tok == this ) {
					tok = cloneAttributes();
				}
				// check for positionIncrement!=1?  if>1, should not match, if==0, check multiple at this level?
				TermAttribute termAtt = tok.getAttribute( TermAttribute.class );
				SynonymMap subMap = map.submap.get( termAtt.termBuffer(), 0, termAtt.termLength() );

				if ( subMap != null ) {
					// recurse
					result = match( subMap );
				}

				if ( result != null ) {
					matched.addFirst( tok );
				}
				else {
					// push back unmatched token
					pushTok( tok );
				}
			}
		}

		// if no longer sequence matched, so if this node has synonyms, it's the match.
		if ( result == null && map.synonyms != null ) {
			result = map;
		}

		return result;
	}

	private void copy(AttributeSource target, AttributeSource source) {
		if ( target != source ) {
			if ( source.hasAttributes() ) {
				State sourceState = source.captureState();
				target.restoreState( sourceState );
			}
		}
	}

	@Override
	public void reset() throws IOException {
		input.reset();
		replacement = null;
	}
}
