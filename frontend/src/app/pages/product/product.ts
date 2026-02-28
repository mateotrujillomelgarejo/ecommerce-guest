import { Component, inject, computed, ChangeDetectionStrategy, signal, effect } from '@angular/core';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { ProductService } from '../../services/product.service';
import { CartService } from '../../services/cart.service';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { ProductDetailDTO } from '../../models/product';

@Component({
  selector: 'app-product',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatIconModule],
  template: `
    <header class="header">
      <a routerLink="/" class="logo">
        <img src="https://res.cloudinary.com/dks2ofiqn/image/upload/v1766587808/logo_tp7vmy.png" alt="Purple Sky logo" referrerPolicy="no-referrer">
        <div>
          <div class="brand">Purple Sky</div>
          <div class="subbrand">Accesorios · Anillos · Regalos · Joyas</div>
        </div>
      </a>
      <a routerLink="/cart" class="cart-btn">
        <mat-icon>shopping_cart</mat-icon>
        <span class="cart-count-js cart-count">{{ cartService.cartCount() }}</span>
      </a>
    </header>

    <main class="container">
      <div id="productDetail" class="product-detail">
        @if (productDetail()) {
          <div class="product-card-detail">
            <div class="product-image-container" style="display: flex; flex-direction: column; gap: 12px;">
              <div class="product-image" style="width: 100%; aspect-ratio: 1; border-radius: 16px; overflow: hidden; background: var(--bg);">
                <img [src]="selectedImage() || productDetail()!.product.images[0] || 'https://picsum.photos/seed/' + productDetail()!.product.id + '/400/400'" 
                     [alt]="productDetail()!.product.name" 
                     style="width: 100%; height: 100%; object-fit: cover;"
                     referrerPolicy="no-referrer">
              </div>
              
              @if (productDetail()!.product.images && productDetail()!.product.images.length > 1) {
                <div class="image-thumbnails" style="display: flex; gap: 8px; overflow-x: auto; padding-bottom: 4px;">
                  @for (img of productDetail()!.product.images; track img) {
                    <button 
                      (click)="selectedImage.set(img)"
                      style="width: 60px; height: 60px; border-radius: 8px; overflow: hidden; border: 2px solid transparent; cursor: pointer; padding: 0; background: var(--bg); flex-shrink: 0;"
                      [style.borderColor]="selectedImage() === img || (!selectedImage() && $first) ? 'var(--accent)' : 'transparent'">
                      <img [src]="img" [alt]="productDetail()!.product.name" style="width: 100%; height: 100%; object-fit: cover;" referrerPolicy="no-referrer">
                    </button>
                  }
                </div>
              }
            </div>
            
            <div class="product-info">
              <h2>{{ productDetail()!.product.name }}</h2>
              
              @if (productDetail()!.product.averageRating) {
                <div style="display: flex; align-items: center; gap: 4px; margin-bottom: 8px; color: #f59e0b; font-size: 14px;">
                  <mat-icon style="font-size: 16px; width: 16px; height: 16px;">star</mat-icon>
                  <span style="font-weight: 600; color: var(--fg);">{{ productDetail()!.product.averageRating }}</span>
                </div>
              }

              <div class="price">S/ {{ productDetail()!.product.price.toFixed(2) }}</div>
              <p class="desc">{{ productDetail()!.product.description }}</p>

              @if (productDetail()!.product.tags && productDetail()!.product.tags!.length > 0) {
                <div style="display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 24px;">
                  @for (tag of productDetail()!.product.tags; track tag) {
                    <span style="background: var(--bg); padding: 4px 10px; border-radius: 100px; font-size: 12px; color: var(--muted);">#{{ tag }}</span>
                  }
                </div>
              }

              <ul class="product-specs">
                <li><strong>Categoría:</strong> <span style="text-transform: capitalize;">{{ productDetail()!.product.category }}</span></li>
                <li>
                  <strong>Disponibilidad:</strong> 
                  <span [style.color]="productDetail()!.available ? 'var(--success)' : 'var(--error)'" style="font-weight: 600;">
                    {{ productDetail()!.stockMessage || (productDetail()!.available ? 'En stock' : 'Agotado') }}
                  </span>
                </li>
                <li><strong>Envío:</strong> <span>A todo el Perú</span></li>
                <li><strong>Garantía:</strong> <span>30 días por defecto de fábrica</span></li>
              </ul>

              <div class="btn-group">
                <button class="btn add-js" (click)="addToCart()" [disabled]="!productDetail()!.available">
                  @if (added()) {
                    <mat-icon style="font-size: 18px; width: 18px; height: 18px;">check</mat-icon> Agregado
                  } @else {
                    <mat-icon style="font-size: 18px; width: 18px; height: 18px;">add_shopping_cart</mat-icon> Agregar al carrito
                  }
                </button>
                <a routerLink="/" class="btn ghost">
                  <mat-icon style="font-size: 18px; width: 18px; height: 18px;">storefront</mat-icon> Volver al catálogo
                </a>
              </div>
            </div>
          </div>
        } @else {
          <div class="card"><div style="text-align:center; padding: 40px 0;">Cargando producto... <br><br><a routerLink="/" class="btn">Volver al catálogo</a></div></div>
        }
      </div>

      @if (relatedProducts().length > 0) {
        <section id="relatedProducts" class="related-section">
          <h3>También podría interesarte</h3>
          <div class="grid" id="relatedGrid">
            @for (r of relatedProducts(); track r.id) {
              <div class="card">
                <img [src]="r.images[0] || 'https://picsum.photos/seed/' + r.id + '/400/400'" [alt]="r.name" referrerPolicy="no-referrer">
                <h4>{{ r.name }}</h4>
                <div style="display:flex; justify-content:space-between; align-items:flex-end; margin-top: auto;">
                  <div class="price">S/ {{ r.price.toFixed(2) }}</div>
                  <a [routerLink]="['/product', r.id]" class="btn ghost view-js">Ver más</a>
                </div>
              </div>
            }
          </div>
        </section>
      }
    </main>

    <footer class="footer">
      Purple Sky &bull; Envíos internacionales · Pedidos con 1 día de anticipación · RUC 10705538978
    </footer>
  `
})
export class ProductComponent {
  productService = inject(ProductService);
  cartService = inject(CartService);
  route = inject(ActivatedRoute);

  paramMap = toSignal(this.route.paramMap);
  
  productId = computed(() => this.paramMap()?.get('id') || '');
  
  productDetail = signal<ProductDetailDTO | null>(null);
  selectedImage = signal<string | null>(null);

  constructor() {
    effect(() => {
      const id = this.productId();
      if (id) {
        this.selectedImage.set(null);
        this.productService.getProductDetails(id).subscribe({
          next: p => {
            this.productDetail.set(p);
            if (p.product.images && p.product.images.length > 0) {
              this.selectedImage.set(p.product.images[0]);
            }
          },
          error: err => {
            console.error('Product not found', err);
            this.productDetail.set(null);
          }
        });
      }
    });
  }

  relatedProducts = computed(() => {
    const p = this.productDetail()?.product;
    if (!p) return [];
    return this.productService.products().filter(x => x.category === p.category && x.id !== p.id);
  });

  added = signal(false);

  addToCart() {
    const detail = this.productDetail();
    if (detail && detail.available) {
      this.cartService.addToCart(detail.product);
      this.added.set(true);
      setTimeout(() => this.added.set(false), 900);
    }
  }
}
