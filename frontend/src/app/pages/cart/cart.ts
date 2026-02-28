import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartService } from '../../services/cart.service';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-cart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, FormsModule, MatIconModule],
  template: `
    <header class="header">
      <a routerLink="/" class="logo">
        <img src="https://picsum.photos/seed/logo/100/100" alt="Purple Sky logo" referrerPolicy="no-referrer">
        <div>
          <div class="brand">Purple Sky</div>
          <div class="subbrand">Carrito</div>
        </div>
      </a>
      <a routerLink="/" class="btn ghost">
        <mat-icon style="font-size: 18px; width: 18px; height: 18px;">arrow_back</mat-icon> Seguir comprando
      </a>
    </header>

    <main class="container">
      <div class="topbar">
        <h1>Tu carrito</h1>
      </div>
      <section class="cart-section">
        <div id="cartList" class="cart-list">
          @if (cartService.items().length === 0) {
            <div class="card"><div style="text-align:center; padding: 40px 0;">Tu carrito está vacío. <br><br><a routerLink="/" class="btn">Volver al catálogo</a></div></div>
          } @else {
            @for (item of cartService.items(); track item.productId) {
              <div class="cart-item">
                <img [src]="item.imageUrl || 'https://picsum.photos/seed/' + item.productId + '/100/100'" [alt]="item.productName" referrerPolicy="no-referrer">
                <div style="flex:1">
                  <div style="font-weight:700; font-size: 18px;">{{ item.productName }}</div>
                  <div class="small">S/ {{ item.price.toFixed(2) }}</div>
                </div>
                <div>
                  <input class="qty" type="number" min="1" [ngModel]="item.quantity" (ngModelChange)="updateQty(item.productId, $event)">
                </div>
                <div style="display:flex; flex-direction:column; gap:6px; align-items:flex-end; margin-left: auto;">
                  <div style="font-weight:700; font-size: 18px;">S/ {{ item.subtotal.toFixed(2) }}</div>
                  <button class="btn ghost remove-js" (click)="cartService.removeFromCart(item.productId)" style="padding: 6px 12px; font-size: 13px; color: var(--accent);">
                    <mat-icon style="font-size: 16px; width: 16px; height: 16px;">delete</mat-icon> Eliminar
                  </button>
                </div>
              </div>
            }
          }
        </div>
        
        @if (cartService.items().length > 0) {
          <div id="cartSummary" style="margin-top: 24px;">
            <div class="summary">
              <div style="display:flex; justify-content:space-between; margin-bottom: 12px;"><div>Subtotal</div><div>S/ {{ cartService.subtotal().toFixed(2) }}</div></div>
              <div style="display:flex; justify-content:space-between; margin-bottom: 12px;"><div>Envío estimado</div><div>S/ {{ (cartService.cart()?.shippingEstimate || shippingFee).toFixed(2) }}</div></div>
              <hr style="margin:16px 0;border:none;border-top:1px dashed var(--border)">
              <div style="display:flex; justify-content:space-between;font-weight:700; font-size: 20px;"><div>Total</div><div>S/ {{ cartService.cartTotal().toFixed(2) }}</div></div>
              <div style="margin-top:24px; display:flex; gap:12px; flex-wrap: wrap;">
                <a routerLink="/" class="btn ghost" style="flex: 1; text-align: center;">Seguir comprando</a>
                <a routerLink="/checkout" class="btn" style="flex: 1; text-align: center;">
                  Ir a pagar <mat-icon style="font-size: 18px; width: 18px; height: 18px;">arrow_forward</mat-icon>
                </a>
              </div>
            </div>
          </div>
        }
      </section>
    </main>

    <footer class="footer">
      Purple Sky · Si tienes dudas escríbenos en Instagram @purple_sky_c
    </footer>
  `
})
export class CartComponent {
  cartService = inject(CartService);
  shippingFee = 8.00;

  updateQty(id: string, qty: number) {
    this.cartService.updateQuantity(id, qty);
  }
}
