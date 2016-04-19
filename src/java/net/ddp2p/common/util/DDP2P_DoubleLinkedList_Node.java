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
public class DDP2P_DoubleLinkedList_Node<T> {
	public DDP2P_DoubleLinkedList_Node<T> next;
	public DDP2P_DoubleLinkedList_Node<T> previous;
	public T payload;
	/**
	 * Initialize with fields pointing to itself
	 */
	public DDP2P_DoubleLinkedList_Node(){
		previous = next = this;
	}
	/**
	 * 
	 * @param _prev
	 * @param _next
	 */
	public DDP2P_DoubleLinkedList_Node(
			DDP2P_DoubleLinkedList_Node<T> _prev,
			DDP2P_DoubleLinkedList_Node<T> _next,
			T _payload){
		next=_next;
		previous =_prev;
		payload = _payload;
	}
	public DDP2P_DoubleLinkedList_Node<T> getNext() {
		return next;
	}
}
