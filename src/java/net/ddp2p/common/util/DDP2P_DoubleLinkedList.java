/*   Copyright (C) 2013 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
package net.ddp2p.common.util;
/**
 * Initialized with an empty node in head
 * T has to implement DDP2P_DoubleLinkedList_Node_Payload (to access its node)
 * 
 * @author msilaghi
 *
 * @param <T>
 */
public class DDP2P_DoubleLinkedList<T> {
	public static interface CachedSummary {
		public String getCachedSummary();
	}
	private static final boolean DEBUG = false;
	DDP2P_DoubleLinkedList_Node<T> head = new DDP2P_DoubleLinkedList_Node<T>();
	/**
	 * Number of elements
	 */
	int count = 0;
	public String toStringHead() {
		String s = "";
		DDP2P_DoubleLinkedList_Node<T> crt = head;
		do {
			s += "["+crt.payload+"]";
			crt = crt.next;
		} while (crt != head);
		return s;
	}
	public String toString() {
		String s = "";
		DDP2P_DoubleLinkedList_Node<T> crt = head;
		if (head.payload != null) {
			s += "[H["+head.payload+"]]";
		}
		crt = crt.next;
		while (crt != head) {
			s += "["+crt.payload+"]";
			crt = crt.next;
		};
		return s;
	}
	public String toStringGIDH() {
		String s = "";
		DDP2P_DoubleLinkedList_Node<T> crt = head;
		if (head.payload != null) {
			if (head.payload instanceof CachedSummary)
				s += "[H["+((CachedSummary)head.payload).getCachedSummary()+"]]";
		}
		crt = crt.next;
		while (crt != head) {
			if (head.payload instanceof CachedSummary)
				s += "["+((CachedSummary)crt.payload).getCachedSummary()+"]";
			crt = crt.next;
		};
		return s;
	}
	/**
	 * Just the head
	 */
	public DDP2P_DoubleLinkedList() {
	}
	/**
	 * Insert crt in head :)
	 *  "crt" should not have been in any DDP2P_DoubleLinkedList list
	 * 
	 * @param crt
	 * @return : returns true on success, false on failure
	 */
	synchronized public boolean offerFirst(T crt) {
		if (crt == null) {
			System.out.println("DDP2P_DoubleLinkedList:offerFirst: trying to register null");
			return false;
		}
		if (! (crt instanceof DDP2P_DoubleLinkedList_Node_Payload<?>)) {
			System.out.println("DDP2P_DoubleLinkedList:offerFirst: Wrong payload for DDP2P_DoubleLinkedList");
			Util.printCallPath("here");
			return false;
		}
		@SuppressWarnings("unchecked")
		DDP2P_DoubleLinkedList_Node_Payload<T> p = (DDP2P_DoubleLinkedList_Node_Payload<T>) crt;
		/**
		 * If in some list, return false
		 */
		if (p.get_DDP2P_DoubleLinkedList_Node() != null) {
			System.out.println("DDP2P_DoubleLinkedList:offerFirst: Already in some list!!!!!!");
			Util.printCallPath("here");
			return false;
		}
		/**
		 * 
		 */
		DDP2P_DoubleLinkedList_Node<T> ins = new DDP2P_DoubleLinkedList_Node<T>(head, head.next, crt);
		head.next.previous = ins;
		head.next = ins;
		count ++;
		p.set_DDP2P_DoubleLinkedList_Node(ins);
		return true;
	}
	/**
	 * Return the count
	 * @return
	 */
	public int size() {
		return count;
	}
	synchronized public T removeTail() {
		if (count <= 0) return null;
		count --;
		DDP2P_DoubleLinkedList_Node<T> last = head.previous;
		DDP2P_DoubleLinkedList_Node<T> new_last = last.previous;
		head.previous = new_last;
		new_last.next = head;
		T crt = last.payload;
		@SuppressWarnings("unchecked")
		DDP2P_DoubleLinkedList_Node_Payload<T> p = (DDP2P_DoubleLinkedList_Node_Payload<T>) crt;
		if (last != head) { 
			last.previous = null;
			last.next = null;
		}
		p.set_DDP2P_DoubleLinkedList_Node(null);
		return crt;
	}
	/**
	 * Makes crt the first element in the List.
	 * Throws runtimeException if not yet in the list.
	 * 
	 * 
	 * @param crt
	 * @return true if already first or added, false if not a legitimate payload
	 */
	synchronized public boolean moveToFront(T crt) {
		if (! (crt instanceof DDP2P_DoubleLinkedList_Node_Payload<?>)) {
			System.out.println("DDP2P_DoubleLinkedList:offerFirst: Wrong payload for DDP2P_DoubleLinkedList");
			return false;
		}
		@SuppressWarnings("unchecked")
		DDP2P_DoubleLinkedList_Node_Payload<T> p = (DDP2P_DoubleLinkedList_Node_Payload<T>) crt;
		DDP2P_DoubleLinkedList_Node<T> ins = p.get_DDP2P_DoubleLinkedList_Node();
		if (ins == null) {
			throw new RuntimeException("Node was not in List");
		}
		if (ins == head.next) return true; 
		DDP2P_DoubleLinkedList_Node<T> prev = ins.previous;
		DDP2P_DoubleLinkedList_Node<T> next = ins.next;
		if (prev != null) prev.next = next;
		if (next != null) next.previous = prev;
		DDP2P_DoubleLinkedList_Node<T> old_first = head.next;
		ins.next = old_first;
		ins.previous = head;
		head.next = ins;
		old_first.previous = ins;
		return true;
	}
	public void remove(T crt) {
		if (count <= 0 ) return;
		count --;
		if (! (crt instanceof DDP2P_DoubleLinkedList_Node_Payload<?>)) {
			System.out.println("DDP2P_DoubleLinkedList:remove: Wrong payload for DDP2P_DoubleLinkedList");
			return;
		}
		@SuppressWarnings("unchecked")
		DDP2P_DoubleLinkedList_Node_Payload<T> p = (DDP2P_DoubleLinkedList_Node_Payload<T>) crt;
		DDP2P_DoubleLinkedList_Node<T> ins = p.get_DDP2P_DoubleLinkedList_Node();
		if (ins == null) {
			throw new RuntimeException("Node was not in List");
		}
		DDP2P_DoubleLinkedList_Node<T> prev = ins.previous;
		DDP2P_DoubleLinkedList_Node<T> next = ins.next;
		if (prev != null) prev.next = next;
		if (next != null) next.previous = prev;
		if (ins != head) { 
			ins.next = null;
			ins.previous = null;
		}
		p.set_DDP2P_DoubleLinkedList_Node(null); 
	}
	public boolean inListProbably(T crt) {
		if (!(crt instanceof DDP2P_DoubleLinkedList_Node_Payload<?>)) {
			System.out.println("DDP2P_DoubleLinkedList: inList: Wrong payload for DDP2P_DoubleLinkedList");
			return false;
		}
		@SuppressWarnings("unchecked")
		DDP2P_DoubleLinkedList_Node_Payload<T> p = (DDP2P_DoubleLinkedList_Node_Payload<T>) crt;
		DDP2P_DoubleLinkedList_Node<T> ins = p.get_DDP2P_DoubleLinkedList_Node();
		if (ins == null) {
			if (DEBUG) System.out.println("DDP2P_DoubleLinkedList: inList: Node was not in List");
			return false;
		}
		return true;
	}
	public T getTail() {
		return head.previous.payload;
	}
	public DDP2P_DoubleLinkedList_Node<T> getHead() {
		return head;
	}
}
