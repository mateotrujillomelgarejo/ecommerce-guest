import { Component, inject, ChangeDetectionStrategy, signal } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { CartService } from '../../services/cart.service';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { HttpClient } from '@angular/common/http';
import { switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-checkout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, FormsModule, MatIconModule],
  template: `
    <header class="header">
      <a routerLink="/" class="logo">
        <img src="https://picsum.photos/seed/logo/100/100" alt="Purple Sky logo" referrerPolicy="no-referrer">
        <div>
          <div class="brand">Purple Sky</div>
          <div class="subbrand">Detalle de compra</div>
        </div>
      </a>
      <div style="margin-left:auto;">
        <a routerLink="/cart" class="btn ghost">
          <mat-icon style="font-size: 18px; width: 18px; height: 18px;">arrow_back</mat-icon> Volver al carrito
        </a>
      </div>
    </header>

    <main class="container">
      <div class="topbar">
        <h1>Formulario de compra</h1>
      </div>
      <div style="display:grid; grid-template-columns: 1fr 360px; gap:32px; margin-top:12px; align-items: start;">
        <div>
          <form id="checkoutForm" (ngSubmit)="onSubmit()" class="card" style="padding: 24px;">
            <h3 style="margin-top: 0; margin-bottom: 20px; font-size: 18px; border-bottom: 1px solid var(--border); padding-bottom: 12px;">Datos de contacto</h3>
            <div class="form-row">
              <div class="form-group">
                <label for="fullname">Nombre completo</label>
                <input id="fullname" name="fullname" type="text" [(ngModel)]="formData().fullname" required>
              </div>
              <div class="form-group">
                <label for="email">Email</label>
                <input id="email" name="email" type="email" [(ngModel)]="formData().email" required>
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label for="phone">Teléfono</label>
                <input id="phone" name="phone" type="tel" [(ngModel)]="formData().phone" required>
              </div>
              <div class="form-group">
                <label for="country">País</label>
                <input id="country" name="country" type="text" [(ngModel)]="formData().country" required>
              </div>
            </div>

            <h3 style="margin-top: 12px; margin-bottom: 20px; font-size: 18px; border-bottom: 1px solid var(--border); padding-bottom: 12px;">Dirección de envío</h3>
            <div class="form-group">
              <label for="address">Dirección (calle, número, referencia)</label>
              <input id="address" name="address" type="text" [(ngModel)]="formData().address" required>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label for="city">Ciudad</label>
                <input id="city" name="city" type="text" [(ngModel)]="formData().city" required>
              </div>
              <div class="form-group">
                <label for="postal">Código postal (opcional)</label>
                <input id="postal" name="postal" type="text" [(ngModel)]="formData().postal">
              </div>
            </div>

            <div class="form-group">
              <label for="notes">Notas para el envío (opcional)</label>
              <textarea id="notes" name="notes" [(ngModel)]="formData().notes" placeholder="Ej: Dejar en portería..."></textarea>
            </div>

            <h3 style="margin-top: 12px; margin-bottom: 20px; font-size: 18px; border-bottom: 1px solid var(--border); padding-bottom: 12px;">Pago</h3>
            <div class="form-group">
              <label for="payment">Método de pago</label>
              <select id="payment" name="payment" [(ngModel)]="formData().payment">
                <option value="CARD">Tarjeta de crédito/débito (simulado)</option>
                <option value="PAYPAL">Pago por PayPal (simulado)</option>
                <option value="COD">Pago contra entrega</option>
              </select>
            </div>

            <div style="display:flex; gap:12px; margin-top:24px;">
              <button class="btn" type="submit" style="flex: 1; padding: 14px; font-size: 16px;" [disabled]="loading()">
                @if (loading()) {
                  <mat-icon style="font-size: 20px; width: 20px; height: 20px;" class="animate-spin">autorenew</mat-icon> Procesando...
                } @else {
                  <mat-icon style="font-size: 20px; width: 20px; height: 20px;">check_circle</mat-icon> Confirmar y pagar
                }
              </button>
            </div>
          </form>
        </div>

        <aside>
          <div id="checkoutSummary" class="summary" style="position: sticky; top: 100px;">
            <h3 style="margin-top: 0; margin-bottom: 20px; font-size: 18px; border-bottom: 1px solid var(--border); padding-bottom: 12px;">Resumen de compra</h3>
            @if (cartService.items().length === 0) {
              <div class="small">Tu carrito está vacío. <a routerLink="/">Volver al catálogo</a></div>
            } @else {
              <div style="max-height: 300px; overflow-y: auto; margin-bottom: 16px; padding-right: 8px;">
                @for (item of cartService.items(); track item.productId) {
                  <div style="display:flex; justify-content:space-between; margin-bottom:12px; font-size: 14px; align-items: center; gap: 12px;">
                    <div style="display: flex; align-items: center; gap: 8px; flex: 1;">
                      <img [src]="item.imageUrl || 'https://picsum.photos/seed/' + item.productId + '/40/40'" [alt]="item.productName" style="width: 40px; height: 40px; border-radius: 6px; object-fit: cover;" referrerPolicy="no-referrer">
                      <div>
                        <div style="font-weight: 500; line-height: 1.2;">{{ item.productName }}</div>
                        <div style="color: var(--muted); font-size: 12px;">Cant: {{ item.quantity }}</div>
                      </div>
                    </div>
                    <div style="font-weight: 600;">S/ {{ item.subtotal.toFixed(2) }}</div>
                  </div>
                }
              </div>
              <div style="background: var(--bg); padding: 16px; border-radius: 12px;">
                <div style="display:flex; justify-content:space-between; margin-bottom: 8px; font-size: 14px; color: var(--muted);">
                  <div>Subtotal</div><div>S/ {{ cartService.subtotal().toFixed(2) }}</div>
                </div>
                <div style="display:flex; justify-content:space-between; margin-bottom: 8px; font-size: 14px; color: var(--muted);">
                  <div>Envío</div><div>S/ {{ (cartService.cart()?.shippingEstimate || shippingFee).toFixed(2) }}</div>
                </div>
                <hr style="margin:12px 0;border:none;border-top:1px dashed var(--border)">
                <div style="display:flex; justify-content:space-between; font-weight:700; font-size: 18px;">
                  <div>Total</div><div style="color: var(--accent);">S/ {{ cartService.cartTotal().toFixed(2) }}</div>
                </div>
              </div>
            }
          </div>
        </aside>
      </div>
    </main>

    <footer class="footer">Purple Sky · Datos requeridos para entrega y contacto</footer>
  `
})
export class CheckoutComponent {
  cartService = inject(CartService);
  router = inject(Router);
  http = inject(HttpClient);
  
  shippingFee = 8.00;
  loading = signal(false);

  formData = signal({
    fullname: '',
    email: '',
    phone: '',
    country: '',
    address: '',
    city: '',
    postal: '',
    notes: '',
    payment: 'CARD'
  });

  onSubmit() {
    const data = this.formData();
    if(data.fullname.trim().length < 3){ alert('Ingrese su nombre completo'); return; }
    if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.email)){ alert('Email inválido'); return; }
    if(data.phone.trim().length < 6){ alert('Ingrese teléfono válido'); return; }
    if(data.address.trim().length < 5){ alert('Ingrese dirección completa'); return; }
    if(data.city.trim().length < 2){ alert('Ingrese la ciudad'); return; }
    if(data.country.trim().length < 2){ alert('Ingrese el país'); return; }

    this.loading.set(true);

    // 1. Create Guest
    this.http.post<{guestId: string, name: string, email: string, phone: string}>('http://localhost:8080/guests', {
      sessionId: this.cartService.sessionId(),
      name: data.fullname.trim(),
      email: data.email.trim(),
      phone: data.phone.trim()
    }).pipe(
      // 2. Add Address to Guest
      switchMap(guest => {
        return this.http.post(`http://localhost:8080/guests/${guest.guestId}/address`, {
          street: data.address.trim(),
          city: data.city.trim(),
          postalCode: data.postal.trim() || '00000',
          country: data.country.trim()
        }).pipe(
          switchMap(() => {
            // 3. Create Pending Order
            return this.http.post<{orderId: string, totalAmount: number, createdAt: string}>('http://localhost:8080/orders/pending', {
              guestId: guest.guestId,
              sessionId: this.cartService.sessionId(),
              total: this.cartService.cartTotal(),
              shippingCost: this.cartService.cart()?.shippingEstimate || this.shippingFee,
              items: this.cartService.items().map(i => ({
                productId: i.productId,
                quantity: i.quantity,
                price: i.price,
                productName: i.productName,
                imageUrl: i.imageUrl
              }))
            }).pipe(
              switchMap(order => {
                // 4. Initiate Payment
                return this.http.post<{paymentId: string}>('http://localhost:8080/payments/initiate', {
                  orderId: order.orderId,
                  amount: order.totalAmount,
                  guestEmail: guest.email,
                  paymentMethod: data.payment,
                  returnUrl: window.location.origin + '/success'
                }).pipe(
                  switchMap(payment => {
                    // 5. Confirm Payment
                    return this.http.post(`http://localhost:8080/payments/confirm/${payment.paymentId}`, {}, {responseType: 'text'})
                      .pipe(
                        switchMap(async () => {
                          return { guest, order };
                        })
                      );
                  })
                );
              })
            );
          })
        );
      })
    ).subscribe({
      next: ({ guest, order }) => {
        const orderDetails = {
          id: order.orderId,
          date: order.createdAt,
          buyer: { name: guest.name, email: guest.email, phone: guest.phone, address: data.address, city: data.city, country: data.country },
          items: this.cartService.items().map(i => ({ 
            id: i.productId, 
            name: i.productName, 
            price: i.price, 
            qty: i.quantity,
            imageUrl: i.imageUrl 
          })),
          total: order.totalAmount.toFixed(2)
        };
        sessionStorage.setItem('ps_last_order', JSON.stringify(orderDetails));
        this.cartService.clearCart();
        this.loading.set(false);
        this.router.navigate(['/success']);
      },
      error: (err) => {
        console.error('Checkout error:', err);
        alert('Ocurrió un error durante el proceso de compra. Por favor, inténtalo de nuevo.');
        this.loading.set(false);
      }
    });
  }
}
