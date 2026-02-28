import { Component, ChangeDetectionStrategy, signal, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

interface OrderDetails {
  id: string;
  date: string;
  buyer: { name: string; email: string; phone: string; address: string; city: string; country: string };
  items: { id: string; name: string; price: number; qty: number; imageUrl?: string }[];
  total: string;
}

@Component({
  selector: 'app-success',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatIconModule],
  template: `
    <header class="header">
      <a routerLink="/" class="logo">
        <img src="https://picsum.photos/seed/logo/100/100" alt="Purple Sky logo" referrerPolicy="no-referrer">
        <div>
          <div class="brand">Purple Sky</div>
          <div class="subbrand">¡Gracias por tu compra!</div>
        </div>
      </a>
      <div style="margin-left:auto;">
        <a routerLink="/" class="btn ghost">
          <mat-icon style="font-size: 18px; width: 18px; height: 18px;">storefront</mat-icon> Volver al catálogo
        </a>
      </div>
    </header>

    <main class="container">
      <div class="success-box">
        <div style="display: flex; justify-content: center; margin-bottom: 24px;">
          <div style="width: 80px; height: 80px; background: var(--success); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; box-shadow: 0 8px 24px rgba(16, 185, 129, 0.2);">
            <mat-icon style="font-size: 40px; width: 40px; height: 40px;">check</mat-icon>
          </div>
        </div>
        <h2>Compra realizada</h2>
        <p class="small">Tu pedido fue procesado correctamente. En breve recibirás la confirmación por correo.</p>

        <div id="orderSummary" style="margin-top:32px; text-align: left; background: var(--bg); padding: 24px; border-radius: 16px;">
          @if (!order()) {
            <div class="small" style="text-align: center;">No hay orden reciente. <br><br><a routerLink="/" class="btn">Volver al catálogo</a></div>
          } @else {
            <div style="font-weight:700; margin-bottom:16px; font-size: 18px; display: flex; align-items: center; gap: 8px;">
              <mat-icon style="color: var(--accent);">receipt</mat-icon> Orden {{ order()!.id }}
            </div>
            <div class="small" style="margin-bottom: 24px;">Gracias <strong>{{ order()!.buyer.name }}</strong>, tu compra fue procesada con éxito.</div>
            <div>
              @for (item of order()!.items; track item.id) {
                <div style="display:flex;justify-content:space-between; margin-bottom: 12px; font-size: 15px; align-items: center; gap: 12px;">
                  <div style="display: flex; align-items: center; gap: 8px; flex: 1;">
                    <img [src]="item.imageUrl || 'https://picsum.photos/seed/' + item.id + '/40/40'" [alt]="item.name" style="width: 40px; height: 40px; border-radius: 6px; object-fit: cover;" referrerPolicy="no-referrer">
                    <div>
                      <div style="font-weight: 500; line-height: 1.2;">{{ item.name }}</div>
                      <div style="color: var(--muted); font-size: 12px;">Cant: {{ item.qty }}</div>
                    </div>
                  </div>
                  <div style="font-weight: 600;">S/ {{ (item.price * item.qty).toFixed(2) }}</div>
                </div>
              }
              <hr style="margin:20px 0;border:none;border-top:1px dashed var(--border)">
              <div style="display:flex;justify-content:space-between; font-weight:700; font-size: 20px;">
                <div>Total pagado</div><div style="color: var(--accent);">S/ {{ order()!.total }}</div>
              </div>
            </div>
          }
        </div>

        <div style="margin-top:32px;">
          <a routerLink="/" class="btn" style="padding: 14px 32px; font-size: 16px;">
            <mat-icon style="font-size: 20px; width: 20px; height: 20px;">storefront</mat-icon> Volver al catálogo
          </a>
        </div>
      </div>
    </main>

    <footer class="footer">Purple Sky · Gracias por apoyar el emprendimiento peruano</footer>
  `
})
export class SuccessComponent {
  order = signal<OrderDetails | null>(null);
  private platformId = inject(PLATFORM_ID);

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      const raw = sessionStorage.getItem('ps_last_order');
      if (raw) {
        try {
          this.order.set(JSON.parse(raw));
        } catch (e) {
          console.error(e);
        }
      }
    }
  }
}
