import React, { useEffect, useMemo, useState } from 'react';
import {
  Activity, ArrowDown, ArrowLeft, ArrowRight, BadgeCheck, Box, Check, ChevronDown,
  ChevronRight, ClipboardList, CreditCard, HeartPulse, LogIn, LogOut, Menu, Minus,
  PackageCheck, Plus, Search, ShieldCheck, ShoppingBag, Stethoscope, Truck, UserRound,
  X,
} from 'lucide-react';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';
const categories = [
  { id: null, name: 'Todo el catálogo' },
  { id: 1, name: 'Monitoreo' },
  { id: 2, name: 'Diagnóstico' },
  { id: 3, name: 'Esterilización' },
  { id: 4, name: 'Mobiliario' },
];
const photoUrls = [
  'https://images.unsplash.com/photo-1584982751601-97dcc096659c?auto=format&fit=crop&w=900&q=85',
  'https://images.unsplash.com/photo-1516574187841-cb9cc2ca948b?auto=format&fit=crop&w=900&q=85',
  'https://images.unsplash.com/photo-1584515933487-779824d29309?auto=format&fit=crop&w=900&q=85',
  'https://images.unsplash.com/photo-1580281658628-5c3a2f956c9a?auto=format&fit=crop&w=900&q=85',
  'https://images.unsplash.com/photo-1583912267550-d3c3b2b8c5d6?auto=format&fit=crop&w=900&q=85',
  'https://images.unsplash.com/photo-1579684385127-1ef15d508118?auto=format&fit=crop&w=900&q=85',
];

async function api(path, options = {}) {
  const token = localStorage.getItem('mp-token');
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) {
    if (response.status === 401 && token) {
      localStorage.removeItem('mp-token');
      localStorage.removeItem('mp-user');
      window.dispatchEvent(new Event('mp-session-expired'));
    }
    throw new Error(data?.mensaje || 'No pudimos completar la solicitud. Intenta de nuevo.');
  }
  return data;
}

const money = (value) => new Intl.NumberFormat('es-CO', {
  style: 'currency', currency: 'COP', maximumFractionDigits: 0,
}).format(Number(value || 0));

function readUser() {
  try { return JSON.parse(localStorage.getItem('mp-user') || 'null'); }
  catch { return null; }
}

export default function App() {
  const [user, setUser] = useState(readUser);
  const [products, setProducts] = useState([]);
  const [loadingProducts, setLoadingProducts] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [activeCategory, setActiveCategory] = useState(null);
  const [query, setQuery] = useState('');
  const [view, setView] = useState('catalog');
  const [modal, setModal] = useState('');
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [cart, setCart] = useState(() => {
    try { return JSON.parse(localStorage.getItem('mp-cart') || '[]'); }
    catch { return []; }
  });
  const [toast, setToast] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    loadProducts();
    const expire = () => { setUser(null); setView('catalog'); setModal('login'); };
    window.addEventListener('mp-session-expired', expire);
    return () => window.removeEventListener('mp-session-expired', expire);
  }, []);

  useEffect(() => { localStorage.setItem('mp-cart', JSON.stringify(cart)); }, [cart]);

  useEffect(() => {
    if (!toast) return undefined;
    const timer = setTimeout(() => setToast(''), 3200);
    return () => clearTimeout(timer);
  }, [toast]);

  async function loadProducts() {
    setLoadingProducts(true);
    setLoadError('');
    try {
      const page = await api('/productos?estado=DISPONIBLE&page=0&size=50');
      setProducts(page.contenido || []);
    } catch (error) {
      setLoadError(`${error.message} Comprueba que el backend esté activo en ${API_URL}.`);
    } finally { setLoadingProducts(false); }
  }

  const filteredProducts = useMemo(() => products.filter((product) => {
    const matchesCategory = activeCategory === null || product.categoriaId === activeCategory;
    const matchesText = `${product.nombre} ${product.descripcion || ''}`.toLowerCase().includes(query.toLowerCase());
    return matchesCategory && matchesText;
  }), [products, activeCategory, query]);

  const cartCount = cart.reduce((total, item) => total + item.cantidad, 0);
  const cartTotal = cart.reduce((total, item) => total + Number(item.producto.precio) * item.cantidad, 0);

  function addToCart(product) {
    setCart((items) => {
      const existing = items.find((item) => item.producto.id === product.id);
      if (existing) return items.map((item) => item.producto.id === product.id
        ? { ...item, cantidad: Math.min(item.cantidad + 1, product.stock) } : item);
      return [...items, { producto: product, cantidad: 1 }];
    });
    setToast('Producto añadido al pedido');
  }

  function changeQuantity(productId, nextQuantity) {
    setCart((items) => items.map((item) => item.producto.id === productId
      ? { ...item, cantidad: Math.min(Math.max(1, nextQuantity), item.producto.stock) } : item));
  }

  function signOut() {
    localStorage.removeItem('mp-token');
    localStorage.removeItem('mp-user');
    setUser(null);
    setView('catalog');
    setToast('Sesión cerrada');
  }

  function handleAuth(authData) {
    localStorage.setItem('mp-token', authData.token);
    localStorage.setItem('mp-user', JSON.stringify(authData.usuario));
    setUser(authData.usuario);
    setModal('');
    setToast(`Hola, ${authData.usuario.nombre.split(' ')[0]}`);
  }

  const isAdmin = user?.rol === 'ADMIN';

  return (
    <div className="app-shell">
      <div className="announcement"><span className="announcement-dot" /> Atención especializada para clínicas y profesionales <span className="announcement-separator">·</span> Envíos a toda Colombia</div>
      <header className="site-header">
        <button className="brand" onClick={() => setView('catalog')} aria-label="Medical Printer inicio">
          <span className="brand-mark"><Activity size={21} strokeWidth={2.2} /></span>
          <span className="brand-copy"><strong>medical<span>printer</span></strong><small>TECNOLOGÍA PARA CUIDAR</small></span>
        </button>
        <nav className="main-nav" aria-label="Navegación principal">
          <button className={view === 'catalog' ? 'nav-link active' : 'nav-link'} onClick={() => setView('catalog')}>Equipos</button>
          {user && <button className={view === 'orders' ? 'nav-link active' : 'nav-link'} onClick={() => setView('orders')}>Mis pedidos</button>}
          {isAdmin && <button className={view === 'admin' ? 'nav-link active' : 'nav-link'} onClick={() => setView('admin')}>Administración</button>}
        </nav>
        <div className="header-actions">
          <button className="icon-button search-toggle" aria-label="Buscar productos" onClick={() => { setView('catalog'); document.getElementById('product-search')?.focus(); }}><Search size={19} /></button>
          {user ? (
            <div className="user-menu">
              <button className="user-chip" onClick={() => setView(isAdmin ? 'admin' : 'orders')}><span className="avatar"><UserRound size={16} /></span><span className="user-name">{user.nombre.split(' ')[0]}</span><ChevronDown size={14} /></button>
              <button className="icon-button logout-button" aria-label="Cerrar sesión" title="Cerrar sesión" onClick={signOut}><LogOut size={18} /></button>
            </div>
          ) : <button className="login-button" onClick={() => setModal('login')}><UserRound size={17} /> <span>Ingresar</span></button>}
          <button className="bag-button" aria-label={`Abrir pedido, ${cartCount} productos`} onClick={() => setModal('cart')}><ShoppingBag size={19} /><span>Pedido</span><b>{cartCount}</b></button>
          <button className="mobile-menu" aria-label="Abrir menú" onClick={() => setModal('menu')}><Menu size={21} /></button>
        </div>
      </header>

      <main>
        {view === 'catalog' && <>
          <section className="hero-section">
            <div className="hero-copy">
              <p className="eyebrow"><span /> EQUIPAMIENTO MÉDICO SELECCIONADO</p>
              <h1>La tecnología<br />que <em>cuida</em> de verdad.</h1>
              <p className="hero-description">Equipos confiables para profesionales que ponen a las personas primero.</p>
              <button className="primary-button hero-cta" onClick={() => document.getElementById('catalogo')?.scrollIntoView({ behavior: 'smooth' })}>Explorar equipos <ArrowDown size={17} /></button>
              <div className="hero-trust"><span className="trust-icon"><ShieldCheck size={18} /></span><span>Asesoría experta <i /> Garantía respaldada <i /> Entrega nacional</span></div>
            </div>
            <div className="hero-visual">
              <div className="hero-image" role="img" aria-label="Profesional de la salud trabajando con equipamiento clínico" />
              <div className="hero-photo-caption"><span className="caption-line" /><span>PREPARADOS PARA CUIDAR MEJOR</span></div>
              <div className="hero-note"><span className="note-icon"><HeartPulse size={18} /></span><span><b>Calidad clínica</b><small>Equipos listos para tu práctica</small></span><BadgeCheck className="note-check" size={17} /></div>
              <span className="hero-index">01 <i /> 04</span>
            </div>
            <div className="hero-side-label">MEDICAL PRINTER / COLOMBIA</div>
          </section>

          <section className="benefit-strip" aria-label="Beneficios">
            <div><span className="benefit-icon"><Stethoscope size={20} /></span><span><b>Selección especializada</b><small>Equipos para el día a día clínico</small></span></div>
            <div><span className="benefit-icon"><Truck size={20} /></span><span><b>Despachos nacionales</b><small>Seguimiento de tu pedido</small></span></div>
            <div><span className="benefit-icon"><ShieldCheck size={20} /></span><span><b>Compra segura</b><small>Acompañamiento en cada paso</small></span></div>
          </section>

          <section className="catalog-section" id="catalogo">
            <div className="catalog-heading">
              <div><p className="eyebrow">CATÁLOGO</p><h2>Equipos para cada cuidado</h2><p className="section-description">Encuentra herramientas confiables para tu consulta o institución.</p></div>
              <label className="search-field"><Search size={18} /><input id="product-search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Buscar equipo..." /><kbd>⌘ K</kbd></label>
            </div>
            <div className="catalog-toolbar">
              <div className="category-tabs" role="tablist" aria-label="Categorías de productos">
                {categories.map((category) => <button key={category.name} role="tab" aria-selected={activeCategory === category.id} className={activeCategory === category.id ? 'category-tab selected' : 'category-tab'} onClick={() => setActiveCategory(category.id)}>{category.name}</button>)}
              </div>
              <span className="result-count">{filteredProducts.length} {filteredProducts.length === 1 ? 'equipo' : 'equipos'}</span>
            </div>
            {loadError && <div className="error-banner"><span>{loadError}</span><button onClick={loadProducts}>Reintentar</button></div>}
            {loadingProducts ? <div className="loading-grid">{[1, 2, 3].map((item) => <div className="product-skeleton" key={item}><span /><i /><i /></div>)}</div> : (
              <div className="product-grid">
                {filteredProducts.map((product, index) => <ProductCard key={product.id} product={product} image={photoUrls[index % photoUrls.length]} onAdd={() => addToCart(product)} onDetails={() => { setSelectedProduct(product); setModal('detail'); }} />)}
                {!filteredProducts.length && !loadError && <div className="empty-results"><Box size={28} /><b>No encontramos equipos</b><span>Prueba otra búsqueda o categoría.</span><button className="text-button" onClick={() => { setQuery(''); setActiveCategory(null); }}>Ver todo el catálogo</button></div>}
              </div>
            )}
            <div className="catalog-footer"><span>¿Buscas algo específico?</span><a href="mailto:ventas@medicalprinter.co">Habla con nuestro equipo <ArrowRight size={15} /></a></div>
          </section>
          <section className="closing-band"><div className="closing-symbol"><Activity size={30} /></div><div><p className="eyebrow">CUIDAR EMPIEZA CON CONFIAR</p><h2>Tu práctica merece<br />equipos a la altura.</h2></div><button className="light-button" onClick={() => document.getElementById('catalogo')?.scrollIntoView({ behavior: 'smooth' })}>Ver catálogo <ArrowRight size={17} /></button><span className="closing-decoration">MP<span>.</span></span></section>
        </>}

        {view === 'orders' && <OrdersView onLogin={() => setModal('login')} />}
        {view === 'admin' && isAdmin && <AdminView onToast={setToast} />}
      </main>

      <footer className="site-footer"><div className="footer-brand"><span className="brand-mark"><Activity size={18} /></span><strong>medical<span>printer</span></strong></div><span>Equipos que acompañan el cuidado.</span><small>© 2026 Medical Printer</small></footer>

      {modal === 'cart' && <CartDrawer cart={cart} total={cartTotal} onClose={() => setModal('')} onChange={changeQuantity} onRemove={(id) => setCart((items) => items.filter((item) => item.producto.id !== id))} onCheckout={() => { if (!user) { setModal('login'); return; } setModal('checkout'); }} />}
      {modal === 'login' && <AuthModal onClose={() => setModal('')} onSuccess={handleAuth} />}
      {modal === 'checkout' && <CheckoutModal cart={cart} onClose={() => setModal('cart')} onSuccess={(order) => { setCart([]); setModal(''); setView('orders'); setToast(`Pedido #${order.pedidoId} creado correctamente`); }} />}
      {modal === 'detail' && selectedProduct && <ProductModal product={selectedProduct} image={photoUrls[products.findIndex((item) => item.id === selectedProduct.id) % photoUrls.length]} onClose={() => setModal('')} onAdd={() => { addToCart(selectedProduct); setModal(''); }} />}
      {modal === 'menu' && <MobileNav onClose={() => setModal('')} onNavigate={(next) => { setView(next); setModal(''); }} user={user} isAdmin={isAdmin} />}
      {toast && <div className="toast" role="status"><span><Check size={16} /></span>{toast}</div>}
    </div>
  );
}

function ProductCard({ product, image, onAdd, onDetails }) {
  const category = categories.find((item) => item.id === product.categoriaId)?.name || 'Equipamiento';
  return <article className="product-card">
    <button className="product-image-button" onClick={onDetails} aria-label={`Ver detalle de ${product.nombre}`}>
      <img src={product.imagenUrl || image} alt={product.nombre} loading="lazy" onError={(event) => { event.currentTarget.style.opacity = '0'; }} />
      <span className="image-label">{category}</span><span className="stock-indicator"><i /> Disponible</span>
      <span className="quick-view">Ver detalle <ArrowRight size={14} /></span>
    </button>
    <div className="product-info"><div className="product-meta"><span>MP-{String(product.id).padStart(3, '0')}</span><span>{product.stock < 5 ? `Últimas ${product.stock} unidades` : 'Listo para despacho'}</span></div>
      <button className="product-title" onClick={onDetails}>{product.nombre}</button>
      <p className="product-description">{product.descripcion || 'Equipo clínico seleccionado para acompañar una atención confiable.'}</p>
      <div className="product-bottom"><strong>{money(product.precio)}</strong><button className="add-button" onClick={onAdd} aria-label={`Añadir ${product.nombre} al pedido`} title="Añadir al pedido"><Plus size={19} /></button></div>
    </div>
  </article>;
}

function ProductModal({ product, image, onClose, onAdd }) {
  return <Modal onClose={onClose} className="product-detail-modal"><button className="modal-close" onClick={onClose} aria-label="Cerrar"><X size={20} /></button><img src={product.imagenUrl || image} alt={product.nombre} /><div className="detail-copy"><p className="eyebrow">{categories.find((category) => category.id === product.categoriaId)?.name || 'EQUIPAMIENTO MÉDICO'}</p><h2>{product.nombre}</h2><p>{product.descripcion || 'Equipo clínico confiable para apoyar tu práctica profesional.'}</p><div className="detail-availability"><span className="stock-indicator"><i /> Disponible</span><span>{product.stock} unidades en inventario</span></div><strong className="detail-price">{money(product.precio)}</strong><button className="primary-button full-button" onClick={onAdd}>Añadir al pedido <ShoppingBag size={17} /></button><small className="detail-note"><ShieldCheck size={15} /> Compra con acompañamiento especializado</small></div></Modal>;
}

function CartDrawer({ cart, total, onClose, onChange, onRemove, onCheckout }) {
  return <div className="drawer-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><aside className="cart-drawer" aria-label="Tu pedido"><div className="drawer-header"><div><p className="eyebrow">TU SELECCIÓN</p><h2>Pedido <span>({cart.reduce((sum, item) => sum + item.cantidad, 0)})</span></h2></div><button className="icon-button" onClick={onClose} aria-label="Cerrar pedido"><X size={20} /></button></div>
    {!cart.length ? <div className="cart-empty"><span><ShoppingBag size={25} /></span><h3>Tu pedido está vacío</h3><p>Agrega equipos del catálogo para empezar.</p><button className="primary-button" onClick={onClose}>Explorar equipos <ArrowRight size={16} /></button></div> : <>
      <div className="cart-items">{cart.map(({ producto, cantidad }) => <div className="cart-item" key={producto.id}><img src={producto.imagenUrl || photoUrls[(producto.id - 1) % photoUrls.length]} alt="" /><div className="cart-item-info"><b>{producto.nombre}</b><span>{money(producto.precio)}</span><div className="quantity-control"><button onClick={() => onChange(producto.id, cantidad - 1)} aria-label="Restar"><Minus size={13} /></button><span>{cantidad}</span><button onClick={() => onChange(producto.id, cantidad + 1)} aria-label="Sumar"><Plus size={13} /></button></div></div><button className="remove-item" onClick={() => onRemove(producto.id)} aria-label="Quitar del pedido"><X size={15} /></button></div>)}</div>
      <div className="drawer-summary"><div><span>Subtotal estimado</span><b>{money(total)}</b></div><small>El total final se confirma al procesar tu pedido.</small><button className="primary-button full-button" onClick={onCheckout}>Continuar pedido <ArrowRight size={17} /></button><button className="continue-shopping" onClick={onClose}>Seguir explorando</button></div>
    </>}
  </aside></div>;
}

function AuthModal({ onClose, onSuccess }) {
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ nombre: '', correo: '', telefono: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(event) {
    event.preventDefault(); setError(''); setBusy(true);
    try {
      const path = mode === 'login' ? '/auth/login' : '/auth/registro';
      const body = mode === 'login' ? { correo: form.correo, password: form.password } : form;
      onSuccess(await api(path, { method: 'POST', body: JSON.stringify(body) }));
    } catch (requestError) { setError(requestError.message); }
    finally { setBusy(false); }
  }
  return <Modal onClose={onClose} className="auth-modal"><button className="modal-close" onClick={onClose} aria-label="Cerrar"><X size={20} /></button><div className="auth-mark"><Activity size={22} /></div><p className="eyebrow">MEDICAL PRINTER</p><h2>{mode === 'login' ? 'Qué bueno verte.' : 'Crea tu cuenta.'}</h2><p className="auth-intro">{mode === 'login' ? 'Ingresa para seguir con tu pedido y revisar tus compras.' : 'Regístrate para gestionar tus pedidos de forma sencilla.'}</p>
    <form onSubmit={submit} className="form-stack">{mode === 'register' && <><Field label="Nombre completo" value={form.nombre} onChange={(value) => setForm({ ...form, nombre: value })} required autoComplete="name" /><Field label="Teléfono" value={form.telefono} onChange={(value) => setForm({ ...form, telefono: value })} autoComplete="tel" /></>}<Field label="Correo electrónico" value={form.correo} onChange={(value) => setForm({ ...form, correo: value })} required type="email" autoComplete="email" /><Field label="Contraseña" value={form.password} onChange={(value) => setForm({ ...form, password: value })} required type="password" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} minLength="8" />{error && <p className="form-error" role="alert">{error}</p>}<button className="primary-button full-button" disabled={busy}>{busy ? 'Un momento...' : mode === 'login' ? 'Ingresar' : 'Crear cuenta'} <ArrowRight size={17} /></button></form>
    <p className="auth-switch">{mode === 'login' ? '¿Primera vez aquí?' : '¿Ya tienes una cuenta?'} <button onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError(''); }}>{mode === 'login' ? 'Crear cuenta' : 'Ingresar'}</button></p>
  </Modal>;
}

function CheckoutModal({ cart, onClose, onSuccess }) {
  const [destinatarioTipo, setDestinatarioTipo] = useState('PERSONA');
  const [contacto, setContacto] = useState('');
  const [nit, setNit] = useState('');
  const [empresa, setEmpresa] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function findCompany() {
    setError(''); setEmpresa(null);
    try { setEmpresa(await api(`/empresas?nit=${encodeURIComponent(nit)}`)); }
    catch (requestError) { setError(requestError.message); }
  }
  async function submit(event) {
    event.preventDefault(); setError(''); setBusy(true);
    try {
      const order = await api('/pedidos', {
        method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() },
        body: JSON.stringify({ destinatarioTipo, empresaId: destinatarioTipo === 'EMPRESA' ? empresa?.empresaId : null, contacto, items: cart.map(({ producto, cantidad }) => ({ productoId: producto.id, cantidad })) }),
      });
      onSuccess(order);
    } catch (requestError) { setError(requestError.message); }
    finally { setBusy(false); }
  }
  return <Modal onClose={onClose} className="checkout-modal"><button className="modal-close" onClick={onClose} aria-label="Cerrar"><X size={20} /></button><p className="eyebrow">ÚLTIMO PASO</p><h2>¿A quién va dirigido?</h2><p className="auth-intro">Cuéntanos cómo te contactamos para coordinar el pedido.</p><form onSubmit={submit} className="form-stack">
    <div className="segmented-control"><button type="button" className={destinatarioTipo === 'PERSONA' ? 'chosen' : ''} onClick={() => { setDestinatarioTipo('PERSONA'); setEmpresa(null); }}>Persona</button><button type="button" className={destinatarioTipo === 'EMPRESA' ? 'chosen' : ''} onClick={() => setDestinatarioTipo('EMPRESA')}>Empresa</button></div>
    {destinatarioTipo === 'EMPRESA' && <div className="company-search"><Field label="NIT de la empresa" value={nit} onChange={setNit} required /><button type="button" className="secondary-button" onClick={findCompany}>Buscar</button>{empresa && <p className="company-found"><BadgeCheck size={15} /> {empresa.nombre} · {empresa.nit}</p>}</div>}
    <Field label="Teléfono o persona de contacto" value={contacto} onChange={setContacto} required maxLength="150" placeholder="Ej. 310 555 0142" />
    <div className="checkout-total"><span>Total estimado</span><b>{money(cart.reduce((sum, item) => sum + Number(item.producto.precio) * item.cantidad, 0))}</b></div>
    {error && <p className="form-error" role="alert">{error}</p>}<button className="primary-button full-button" disabled={busy || (destinatarioTipo === 'EMPRESA' && !empresa)}>{busy ? 'Procesando...' : 'Confirmar pedido'} <ArrowRight size={17} /></button>
  </form></Modal>;
}

function OrdersView({ onLogin }) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => { api('/pedidos/mis').then(setOrders).catch((requestError) => setError(requestError.message)).finally(() => setLoading(false)); }, []);
  return <section className="workspace-view"><div className="workspace-header"><div><p className="eyebrow">TU CUENTA</p><h1>Mis pedidos</h1><p>Consulta el estado y el detalle de tus compras.</p></div><button className="secondary-button" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}><ArrowLeft size={16} /> Volver al catálogo</button></div>
    {loading ? <div className="workspace-loading">Cargando tus pedidos...</div> : error ? <ErrorPanel message={error} action={onLogin} /> : orders.length ? <div className="orders-list">{orders.map((order) => <OrderCard key={order.pedidoId} order={order} />)}</div> : <div className="workspace-empty"><span><ClipboardList size={26} /></span><h2>Aún no tienes pedidos</h2><p>Cuando hagas tu primera compra, podrás seguirla desde aquí.</p></div>}
  </section>;
}

function OrderCard({ order, admin = false, onChangeState, onInvoice, invoiced }) {
  const [expanded, setExpanded] = useState(false);
  return <article className="order-card"><div className="order-main"><div className="order-id"><span>Pedido</span><b>#{order.pedidoId}</b></div><div className="order-date"><span>Fecha</span><b>{new Date(order.createdAt).toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' })}</b></div><div className="order-recipient"><span>Destinatario</span><b>{order.destinatario}</b></div><div className="order-total"><span>Total</span><b>{money(order.total)}</b></div><StatusPill status={order.estado} /><button className="order-expand" onClick={() => setExpanded(!expanded)} aria-label={expanded ? 'Ocultar detalle' : 'Ver detalle'}><ChevronDown className={expanded ? 'turned' : ''} size={19} /></button></div>
    {expanded && <div className="order-detail"><div className="order-products">{order.items.map((item) => <div className="order-product" key={item.productoId}><span>{item.producto} <i>× {item.cantidad}</i></span><b>{money(Number(item.precioUnitario) * item.cantidad)}</b></div>)}</div>{admin && <div className="admin-order-actions"><label>Actualizar estado <select defaultValue={order.estado} onChange={(event) => onChangeState(order.pedidoId, event.target.value)}>{['PENDIENTE', 'CONFIRMADO', 'ENVIADO', 'ENTREGADO', 'CANCELADO'].map((state) => <option key={state}>{state}</option>)}</select></label><button className="secondary-button" disabled={invoiced || ['PENDIENTE', 'CANCELADO'].includes(order.estado)} onClick={() => onInvoice(order.pedidoId)}><CreditCard size={15} /> {invoiced ? 'Facturado' : 'Generar factura'}</button></div>}</div>}</article>;
}

function StatusPill({ status }) {
  return <span className={`status-pill status-${status?.toLowerCase()}`}><i />{status?.toLowerCase().replace(/^./, (letter) => letter.toUpperCase())}</span>;
}

function AdminView({ onToast }) {
  const [tab, setTab] = useState('resumen');
  const [summary, setSummary] = useState(null);
  const [orders, setOrders] = useState([]);
  const [invoices, setInvoices] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);

  async function refresh() {
    setLoading(true); setError('');
    try {
      const [nextSummary, nextOrders, nextInvoices, ...productPages] = await Promise.all([
        api('/admin/resumen'), api('/admin/pedidos'), api('/admin/facturas'),
        ...['DISPONIBLE', 'AGOTADO', 'INACTIVO'].map((state) => api(`/productos?estado=${state}&page=0&size=50`)),
      ]);
      setSummary(nextSummary); setOrders(nextOrders); setInvoices(nextInvoices);
      setProducts(productPages.flatMap((page) => page.contenido || []));
    } catch (requestError) { setError(requestError.message); }
    finally { setLoading(false); }
  }
  useEffect(() => { refresh(); }, []);

  async function changeState(id, estado) {
    try { await api(`/admin/pedidos/${id}/estado`, { method: 'PATCH', body: JSON.stringify({ estado }) }); onToast('Estado del pedido actualizado'); await refresh(); }
    catch (requestError) { onToast(requestError.message); await refresh(); }
  }
  async function createInvoice(id) {
    try { await api(`/admin/pedidos/${id}/factura`, { method: 'POST' }); onToast(`Factura creada para el pedido #${id}`); await refresh(); }
    catch (requestError) { onToast(requestError.message); }
  }
  async function saveProduct(event) {
    event.preventDefault(); setSaving(true);
    try {
      await api(`/admin/productos/${editing.id}`, { method: 'PUT', body: JSON.stringify({ nombre: editing.nombre, descripcion: editing.descripcion, precio: Number(editing.precio), stock: Number(editing.stock), estado: editing.estado }) });
      onToast('Producto actualizado'); setEditing(null); await refresh();
    } catch (requestError) { onToast(requestError.message); }
    finally { setSaving(false); }
  }
  const invoicedIds = new Set(invoices.map((invoice) => invoice.pedidoId));

  return <section className="workspace-view admin-workspace"><div className="workspace-header"><div><p className="eyebrow">CENTRO DE OPERACIONES</p><h1>Administración</h1><p>Pedidos, inventario y facturación en un solo lugar.</p></div><button className="secondary-button" onClick={refresh}><Activity size={16} /> Actualizar</button></div>
    <div className="admin-tabs">{[['resumen', 'Resumen'], ['pedidos', 'Pedidos'], ['facturas', 'Facturas'], ['productos', 'Productos']].map(([key, label]) => <button key={key} className={tab === key ? 'selected' : ''} onClick={() => setTab(key)}>{label}</button>)}</div>
    {error && <p className="form-error admin-error">{error}</p>}
    {loading ? <div className="workspace-loading">Cargando información...</div> : <>
      {tab === 'resumen' && <div className="stats-grid">{[['Pedidos totales', summary?.pedidosTotal, PackageCheck], ['Pendientes', summary?.pedidosPendientes, ClipboardList], ['Facturas emitidas', summary?.facturas, CreditCard], ['Equipos disponibles', summary?.productosDisponibles, Box], ['Usuarios', summary?.usuarios, UserRound]].map(([label, value, Icon]) => <div className="stat-card" key={label}><span><Icon size={18} /></span><small>{label}</small><b>{value ?? 0}</b></div>)}</div>}
      {tab === 'pedidos' && <div className="orders-list">{orders.length ? orders.map((order) => <OrderCard key={order.pedidoId} order={order} admin onChangeState={changeState} onInvoice={createInvoice} invoiced={invoicedIds.has(order.pedidoId)} />) : <div className="workspace-empty"><h2>No hay pedidos todavía</h2></div>}</div>}
      {tab === 'facturas' && <div className="table-wrap"><table><thead><tr><th>Factura</th><th>Pedido</th><th>Emisión</th><th>Subtotal</th><th>Impuestos</th><th>Total</th></tr></thead><tbody>{invoices.map((invoice) => <tr key={invoice.facturaId}><td>#{invoice.numero}</td><td>#{invoice.pedidoId}</td><td>{new Date(invoice.fechaEmision).toLocaleDateString('es-CO')}</td><td>{money(invoice.subtotal)}</td><td>{money(invoice.impuestos)}</td><td><b>{money(invoice.total)}</b></td></tr>)}</tbody></table>{!invoices.length && <div className="table-empty">Todavía no hay facturas.</div>}</div>}
      {tab === 'productos' && <div className="inventory-list">{products.map((product) => <div className="inventory-row" key={product.id}><span className="inventory-icon"><Box size={18} /></span><div><b>{product.nombre}</b><small>{product.stock} unidades · {money(product.precio)}</small></div><span className="inventory-state">{product.estado}</span><button className="secondary-button" onClick={() => setEditing({ ...product })}>Editar</button></div>)}</div>}
    </>}
    {editing && <Modal onClose={() => setEditing(null)} className="edit-product-modal"><button className="modal-close" onClick={() => setEditing(null)} aria-label="Cerrar"><X size={20} /></button><p className="eyebrow">INVENTARIO</p><h2>Editar producto</h2><form onSubmit={saveProduct} className="form-stack"><Field label="Nombre" value={editing.nombre} onChange={(value) => setEditing({ ...editing, nombre: value })} required /><label className="field-label">Descripción<textarea value={editing.descripcion || ''} onChange={(event) => setEditing({ ...editing, descripcion: event.target.value })} /></label><div className="field-row"><Field label="Precio (COP)" type="number" min="0" value={editing.precio} onChange={(value) => setEditing({ ...editing, precio: value })} required /><Field label="Stock" type="number" min="0" value={editing.stock} onChange={(value) => setEditing({ ...editing, stock: value })} required /></div><label className="field-label">Estado<select value={editing.estado} onChange={(event) => setEditing({ ...editing, estado: event.target.value })}><option>DISPONIBLE</option><option>AGOTADO</option><option>INACTIVO</option></select></label><button className="primary-button full-button" disabled={saving}>{saving ? 'Guardando...' : 'Guardar cambios'} <Check size={16} /></button></form></Modal>}
  </section>;
}

function ErrorPanel({ message, action }) {
  return <div className="workspace-empty"><span><ShieldCheck size={25} /></span><h2>Necesitas iniciar sesión</h2><p>{message}</p><button className="primary-button" onClick={action}>Ingresar <LogIn size={16} /></button></div>;
}

function Field({ label, value, onChange, ...props }) {
  return <label className="field-label">{label}<input value={value} onChange={(event) => onChange(event.target.value)} {...props} /></label>;
}

function Modal({ children, onClose, className = '' }) {
  useEffect(() => {
    const keydown = (event) => event.key === 'Escape' && onClose();
    window.addEventListener('keydown', keydown);
    return () => window.removeEventListener('keydown', keydown);
  }, [onClose]);
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><section className={`modal-panel ${className}`} role="dialog" aria-modal="true">{children}</section></div>;
}

function MobileNav({ onClose, onNavigate, user, isAdmin }) {
  return <div className="drawer-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><aside className="mobile-nav-panel"><div className="drawer-header"><b>Menú</b><button className="icon-button" onClick={onClose} aria-label="Cerrar menú"><X size={20} /></button></div><button onClick={() => onNavigate('catalog')}>Equipos <ChevronRight size={16} /></button>{user && <button onClick={() => onNavigate('orders')}>Mis pedidos <ChevronRight size={16} /></button>}{isAdmin && <button onClick={() => onNavigate('admin')}>Administración <ChevronRight size={16} /></button>}</aside></div>;
}