import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { Product, CartResponse } from '../models/product';

const SESSION_KEY = 'ps_session_id';

@Injectable({ providedIn: 'root' })
export class CartService {
  private http = inject(HttpClient);
  private platformId = inject(PLATFORM_ID);
  
  cart = signal<CartResponse | null>(null);
  sessionId = signal<string>('');

  items = computed(() => this.cart()?.items || []);
  cartCount = computed(() => this.items().reduce((sum, item) => sum + item.quantity, 0));
  cartTotal = computed(() => this.cart()?.total || 0);
  subtotal = computed(() => this.cart()?.subtotal || 0);

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      let sid = localStorage.getItem(SESSION_KEY);
      if (!sid) {
        sid = 'sess_' + Math.random().toString(36).substring(2, 15);
        localStorage.setItem(SESSION_KEY, sid);
      }
      this.sessionId.set(sid);
      this.loadCart();
    }
  }

  loadCart() {
    if (!this.sessionId()) return;
    this.http.get<CartResponse>(`http://localhost:8080/cart?sessionId=${this.sessionId()}`)
      .subscribe({
        next: res => this.cart.set(res),
        error: err => console.error('Error loading cart', err)
      });
  }

  addToCart(product: Product, quantity = 1) {
    if (!this.sessionId()) return;
    this.http.post<CartResponse>(`http://localhost:8080/cart/items?sessionId=${this.sessionId()}`, { 
      productId: product.id, 
      quantity 
    }).subscribe({
      next: res => this.cart.set(res),
      error: err => console.error('Error adding to cart', err)
    });
  }

  updateQuantity(productId: string, quantity: number) {
    if (!this.sessionId()) return;
    this.http.patch<CartResponse>(`http://localhost:8080/cart/items?sessionId=${this.sessionId()}`, { 
      productId, 
      quantity 
    }).subscribe({
      next: res => this.cart.set(res),
      error: err => console.error('Error updating quantity', err)
    });
  }

  removeFromCart(productId: string) {
    if (!this.sessionId()) return;
    this.http.delete<CartResponse>(`http://localhost:8080/cart/items/${productId}?sessionId=${this.sessionId()}`)
      .subscribe({
        next: res => this.cart.set(res),
        error: err => console.error('Error removing from cart', err)
      });
  }

  clearCart() {
    if (isPlatformBrowser(this.platformId)) {
      const sid = 'sess_' + Math.random().toString(36).substring(2, 15);
      localStorage.setItem(SESSION_KEY, sid);
      this.sessionId.set(sid);
      this.cart.set(null);
      this.loadCart(); // Load a fresh cart for the new session
    }
  }
}
