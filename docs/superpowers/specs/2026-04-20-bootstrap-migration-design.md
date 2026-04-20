# Bootstrap 5.3 Migration Design

## Goal

Migrate the PixelVault project from fully custom CSS to Bootstrap 5.3 (CDN), keeping the existing dark theme UI identical. All inline CSS (`<style>` blocks and `style=""` attributes) will be replaced with Bootstrap utility classes where possible, and remaining custom CSS will be extracted into external static CSS files grouped by page/section.

## Decisions

- **Bootstrap version:** 5.3 via CDN
- **Approach:** Bootstrap-first — use Bootstrap classes as foundation, minimal custom CSS for dark theme and project-specific components
- **CSS grouping:** By page/section (store.css, admin.css, auth.css, etc.)
- **Legacy cleanup:** Delete `global.css` and `bootstrap-override.css` (unused by any active template)

## File Structure

```
static/css/
├── shared.css          ← CSS variables + base reset + custom utilities (accent color, font-mono, etc.)
├── store.css           ← storefront layout: navbar, footer, hero, carousel, product grid, marketplace
├── auth.css            ← login/register split layout
├── product.css         ← product detail: gallery, variants, reviews, star picker
├── cart.css            ← cart layout, items, summary
├── order.css           ← order history, order detail, key delivery, payment result
├── admin.css           ← admin shell: sidebar, KPI cards, tables, forms, page header
```

## Template → CSS Mapping

| Template(s) | CSS files loaded |
|---|---|
| `layout/css.html` (storefront base) | Bootstrap CDN → shared.css → store.css |
| `index.html`, `marketplace.html` | inherits from layout |
| `auth/login.html`, `auth/register.html` | Bootstrap CDN → shared.css → auth.css |
| `product-detail.html` | Bootstrap CDN → shared.css → product.css |
| `cart.html` | Bootstrap CDN → shared.css → cart.css |
| `order-history.html`, `order-detail.html`, `payment-result.html` | Bootstrap CDN → shared.css → order.css |
| `admin/layout/css.html` (admin base) | Bootstrap CDN → shared.css → admin.css |
| All admin content pages | inherits from admin layout |

## CSS Load Order

1. Bootstrap 5.3 CSS (CDN)
2. `shared.css` (variables + base + custom utilities — overrides Bootstrap defaults)
3. Page-specific CSS file

This ensures custom CSS takes precedence over Bootstrap defaults.

## Migration Strategy

### A. Inline `style=""` attributes → Bootstrap utility classes

| Inline style | Bootstrap class |
|---|---|
| `display:flex; gap:8px` | `d-flex gap-2` |
| `margin-top:16px` | `mt-3` |
| `text-align:center` | `text-center` |
| `width:100%` | `w-100` |
| `justify-content:center` | `justify-content-center` |
| `color:var(--accent)` | `text-accent` (custom utility in shared.css) |
| `font-family:var(--font-mono)` | `font-mono` (custom utility in shared.css) |

Custom utilities in `shared.css` will cover project-specific values that Bootstrap doesn't provide (accent color, mono font, specific sizes matching the design).

### B. Inline `<style>` blocks → External CSS files

Each page's embedded `<style>` block moves to its corresponding CSS file. Selectors remain the same; only the location changes.

### C. CSS Variables in `shared.css`

Preserve existing dark theme custom properties and map accent to Bootstrap's primary:

```css
:root {
  --accent: #8B7CF6;
  --bs-primary: #8B7CF6;
  /* ...existing variables preserved */
}
```

### D. Dark Theme

Bootstrap 5.3 supports `data-bs-theme="dark"` on `<html>` or `<body>`. This will be set to handle Bootstrap's built-in dark mode for components (tables, cards, forms, modals).

## Execution Order

### Phase 1: Storefront Layout (3 files)
1. `layout/css.html` — add Bootstrap 5.3 CDN, update CSS includes
2. `layout/header.html` — convert navbar to Bootstrap classes
3. `layout/footer.html` — convert footer to Bootstrap grid/classes

### Phase 2: Storefront Pages (5 files)
4. `index.html` — hero, carousel, product grid (~130 lines inline CSS → store.css)
5. `marketplace.html` — search, filter, product grid → store.css
6. `product-detail.html` — gallery, variants, reviews (~70 lines → product.css)
7. `cart.html` — cart layout, summary → cart.css
8. `auth/login.html` + `auth/register.html` — split layout → auth.css

### Phase 3: Order & Payment (3 files)
9. `order-history.html` + `order-detail.html` + `payment-result.html` → order.css

### Phase 4: Admin (14 files)
10. `admin/layout/css.html` — add Bootstrap CDN, update CSS includes
11. `admin/layout/sidebar.html` — convert to Bootstrap sidebar
12. Admin content pages (dashboard, categories, products, keys, orders, users, coupons) — convert `style=""` attributes

### Phase 5: Cleanup
13. Delete `global.css` and `bootstrap-override.css`
14. Refactor `shared.css` — remove parts moved to page-specific files

## Verification

After each page conversion, visually verify that:
- Dark theme is preserved
- Layout and spacing match original
- Responsive behavior works (mobile/tablet/desktop)
- Interactive elements (dropdowns, carousels, forms) still function
- Font Awesome icons render correctly