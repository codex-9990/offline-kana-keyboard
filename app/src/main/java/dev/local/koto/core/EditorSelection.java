/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto.core;

import java.util.ArrayDeque;

/** Bounded transient cursor metadata. No host text, timestamps, persistence or surrounding reads. */
public final class EditorSelection {
    public static final int MAX_EXPECTED=64;
    public static final class Write {
        private final int beforeStart,beforeEnd,beforeSpanStart,beforeSpanEnd,beforeLength;
        private final boolean beforeActive,beforeMissing;
        private final int oldStart,oldEnd,newPosition,spanStart,spanEnd,length;
        private Write(EditorSelection s,int length,boolean composing) {
            beforeStart=oldStart=s.start; beforeEnd=oldEnd=s.end;
            beforeSpanStart=s.spanStart; beforeSpanEnd=s.spanEnd; beforeLength=s.length;
            beforeActive=s.active; beforeMissing=s.missing;
            int first=s.active ? s.spanStart : s.start>=0 && s.end>=0 ? Math.min(s.start,s.end) : -1;
            long next=first<0 ? -1L : (long)first+length;
            newPosition=next<0 || next>Integer.MAX_VALUE ? -1 : (int)next;
            spanStart=composing ? first : -1; spanEnd=composing ? newPosition : -1; this.length=length;
            s.start=s.end=newPosition; s.spanStart=spanStart; s.spanEnd=spanEnd;
            s.active=composing && length>0; s.length=composing ? length : 0; s.missing=false;
        }
    }
    private final ArrayDeque<Write> expected=new ArrayDeque<>();
    private int start,end,spanStart=-1,spanEnd=-1,length,reportedStart,reportedEnd;
    private boolean active,missing;
    public EditorSelection(int start,int end) { select(start,end); }
    public void select(int first,int last) {
        expected.clear(); start=reportedStart=first; end=reportedEnd=last; finish();
    }
    public Write expect(int length,boolean composing) {
        Write write=new Write(this,length,composing); expected.add(write);
        while (expected.size()>MAX_EXPECTED) expected.remove();
        return write;
    }
    public void reject(Write write) {
        expected.remove(write); start=write.beforeStart; end=write.beforeEnd;
        spanStart=write.beforeSpanStart; spanEnd=write.beforeSpanEnd; length=write.beforeLength;
        active=write.beforeActive; missing=write.beforeMissing;
    }
    public void finish() { active=false; missing=false; spanStart=spanEnd=-1; length=0; }
    public boolean hasSelection() { return start>=0 && end>=0 && start!=end; }
    public boolean recoverRegion() {
        // A successful request is not proof that the host retained its composing span.
        return active && (missing || !expected.isEmpty()) && spanStart>=0 && spanEnd>=spanStart;
    }
    public boolean unknownMissingRegion() { return active && (missing || !expected.isEmpty()) && spanStart<0; }
    public int spanStart() { return spanStart; }
    public int spanEnd() { return spanEnd; }
    public int pendingMetadataCount() { return expected.size(); }
    private void acknowledge(Write write) {
        // Consume the ordered prefix, including updates coalesced into this acknowledgement.
        while (!expected.isEmpty()) if (expected.remove()==write) break;
    }
    /** Returns true only for an unaccounted-for actual cursor/selection move. */
    public boolean update(int oldStart,int oldEnd,int newStart,int newEnd,int candidatesStart,int candidatesEnd) {
        if (newStart<0 || newEnd<0) return false; // Absence of metadata is not evidence of a cursor move.
        boolean range=candidatesStart>=0 && candidatesEnd>=candidatesStart;
        boolean current=newStart==start && newEnd==end;
        Write acknowledged=null;
        boolean earlierOld=false;
        for (Write write:expected) {
            earlierOld|=oldStart==write.oldStart && oldEnd==write.oldEnd;
            if (newStart==newEnd && newStart==write.newPosition
                    && (current || earlierOld || oldStart==reportedStart && oldEnd==reportedEnd)
                    && (!range || candidatesStart==write.spanStart && candidatesEnd==write.spanEnd)) {
                // The earliest matching operation may share a cursor with a later
                // preview/cancel operation. Do not acknowledge that later write early.
                if (acknowledged==null) acknowledged=write;
            }
        }
        if (current && active && range && acknowledged==null && (candidatesStart!=spanStart || candidatesEnd!=spanEnd)) {
            select(newStart,newEnd); return true;
        }
        if (acknowledged!=null || current) {
            if (acknowledged!=null) acknowledge(acknowledged);
            reportedStart=newStart; reportedEnd=newEnd;
            // A delayed acknowledgement must not replace our latest predicted cursor/span.
            if (current && active) missing=!range || candidatesStart!=spanStart || candidatesEnd!=spanEnd;
            return false;
        }
        // finishComposingText can report a duplicate at the last acknowledged cursor
        // while a newer word is pending. No actual move occurred; do not revive an old write.
        if (!range && newStart==oldStart && newEnd==oldEnd
                && newStart==reportedStart && newEnd==reportedEnd) return false;
        if (active && spanStart<0 && newStart==newEnd) {
            if (range && candidatesEnd==newStart && candidatesEnd-candidatesStart==length) {
                spanStart=candidatesStart; spanEnd=candidatesEnd; start=end=newStart; missing=false;
                expected.clear();
                reportedStart=newStart; reportedEnd=newEnd; return false;
            }
            // One acknowledged write can establish the anchor from supplied old/new selection.
            int first=oldStart>=0 && oldEnd>=0 ? Math.min(oldStart,oldEnd) : -1;
            if (!range && expected.size()==1 && first>=0 && (long)first+length==newStart) {
                spanStart=first; spanEnd=newStart; start=end=newStart; missing=true;
                expected.clear();
                reportedStart=newStart; reportedEnd=newEnd; return false;
            }
            // Never guess a replacement range when callbacks provide no trustworthy anchor.
            if (!range) { missing=true; return false; }
        }
        boolean outside=active;
        select(newStart,newEnd); return outside;
    }
}
