import { Component, inject, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../services/product.service';
import { CartService } from '../../services/cart.service';
import { Product } from '../../models/product';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-catalog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, FormsModule, MatIconModule],
  template: `
    <header class="header">
      <a routerLink="/" class="logo">
        <img src="https://res.cloudinary.com/dks2ofiqn/image/upload/v1766587808/logo_tp7vmy.png" alt="Purple Sky logo" referrerPolicy="no-referrer">
        <div>
          <div class="brand">Purple Sky</div>
          <div class="subbrand">Accesorios · Anillos · Regalos · Joyas</div>
        </div>
      </a>

      <div class="search-bar inline">
        <input type="text" [ngModel]="searchTerm()" (ngModelChange)="searchTerm.set($event)" placeholder="Buscar producto...">
      </div>

      <a routerLink="/cart" class="cart-btn">
        <mat-icon>shopping_cart</mat-icon>
        <span class="cart-count-js cart-count">{{ cartService.cartCount() }}</span>
      </a>
    </header>

    <main class="container">
      <div class="topbar">
        <div>
          <h1>Catálogo</h1>
          <p>Regala un accesorio, comparte un sentimiento — Lima / Perú</p>
        </div>
      </div>

      <div class="filters">
        <label>
          Categoría:
          <select [ngModel]="selectedCategory()" (ngModelChange)="selectedCategory.set($event)">
            <option value="">Todas</option>
            <option value="anillos">Anillos</option>
            <option value="collares">Collares</option>
            <option value="aretes">Aretes</option>
            <option value="pulseras">Pulseras</option>
          </select>
        </label>

        <label>
          Precio máximo:
          <input type="number" [ngModel]="maxPrice()" (ngModelChange)="maxPrice.set($event)" placeholder="S/ 50">
        </label>
      </div>

      <div id="catalogGrid" class="grid">
        @for (p of filteredProducts(); track p.id) {
          <div class="card">
            <img [src]="p.images[0] || 'https://picsum.photos/seed/' + p.id + '/400/400'" [alt]="p.name" referrerPolicy="no-referrer">
            <h4>{{ p.name }}</h4>
            <div style="display:flex; justify-content:space-between; align-items:flex-end; margin-top: auto;">
              <div>
                <div class="price">S/ {{ p.price.toFixed(2) }}</div>
                <div style="font-size:13px; color:var(--muted)">{{ p.description }}</div>
              </div>
              <div style="display:flex; flex-direction:column; gap:8px; align-items:flex-end;">
                <button class="btn add-js" (click)="addToCart(p)" [attr.aria-label]="'Agregar ' + p.name">
                  @if (addedState()[p.id]) {
                    <mat-icon style="font-size: 18px; width: 18px; height: 18px;">check</mat-icon> Agregado
                  } @else {
                    <mat-icon style="font-size: 18px; width: 18px; height: 18px;">add_shopping_cart</mat-icon> Agregar
                  }
                </button>
                <a [routerLink]="['/product', p.id]" class="btn ghost view-js">Ver más</a>
              </div>
            </div>
          </div>
        }
      </div>
    </main>

    <footer class="footer">
      Purple Sky &bull; Envíos internacionales · Pedidos con 1 día de anticipación · RUC 10705538978
    </footer>
  `
})
export class CatalogComponent {
  productService = inject(ProductService);
  cartService = inject(CartService);

  searchTerm = signal('');
  selectedCategory = signal('');
  maxPrice = signal<number | null>(null);

  filteredProducts = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const cat = this.selectedCategory().toLowerCase();
    const price = this.maxPrice();

    return this.productService.products().filter(p => {
      const matchesTerm = !term || p.name.toLowerCase().includes(term) || (p.description && p.description.toLowerCase().includes(term));
      const matchesCat = !cat || (p.category && p.category.toLowerCase() === cat);
      const matchesPrice = !price || p.price <= price;
      return matchesTerm && matchesCat && matchesPrice;
    });
  });

  addedState = signal<Record<string, boolean>>({});

  addToCart(product: Product) {
    this.cartService.addToCart(product);
    this.addedState.update(state => ({ ...state, [product.id]: true }));
    setTimeout(() => {
      this.addedState.update(state => ({ ...state, [product.id]: false }));
    }, 900);
  }
}
