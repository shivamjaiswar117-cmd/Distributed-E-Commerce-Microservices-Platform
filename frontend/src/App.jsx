
import { useEffect, useMemo, useState } from 'react'
import './App.css'

const API = '/api'
const USER_ID = 1

const money = amount =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(Number(amount) || 0)

function decorateProduct(product) {
  const name = (product.name || '').toLowerCase()

  let icon = '🛍️'
  let color = '#e8eaff'

  if (name.includes('laptop')) {
    icon = '💻'
    color = '#e8eaff'
  } else if (name.includes('mouse')) {
    icon = '🖱️'
    color = '#e3f4ee'
  } else if (name.includes('headphone')) {
    icon = '🎧'
  } else if (name.includes('watch')) {
    icon = '⌚'
  }

  return {
    ...product,
    category: product.category || 'Electronics',
    icon,
    color,
    price: Number(product.price) || 0,
    stockQuantity: Number(product.stockQuantity) || 0,
  }
}

function App() {
  const [products, setProducts] = useState([])
  const [cart, setCart] = useState({})
  const [category, setCategory] = useState('All')
  const [search, setSearch] = useState('')
  const [showCart, setShowCart] = useState(false)
  const [notice, setNotice] = useState('')
  const [loading, setLoading] = useState(true)
  const [checkingOut, setCheckingOut] = useState(false)

  useEffect(() => {
    async function loadProducts() {
      try {
        const response = await fetch(`${API}/products`)
        if (!response.ok) throw new Error('Could not load products')

        const data = await response.json()
        const list = Array.isArray(data) ? data : data.value || []

        setProducts(list.map(decorateProduct))
      } catch {
        setNotice('Unable to load products. Check that your backend is running.')
      } finally {
        setLoading(false)
      }
    }

    loadProducts()
  }, [])

  const categories = useMemo(
    () => ['All', ...new Set(products.map(product => product.category))],
    [products]
  )

  const filteredProducts = products.filter(product =>
    (category === 'All' || product.category === category) &&
    `${product.name} ${product.description || ''}`
      .toLowerCase()
      .includes(search.toLowerCase())
  )

  const cartItems = products.filter(product => (cart[product.id] || 0) > 0)

  const cartCount = Object.values(cart).reduce(
    (sum, quantity) => sum + quantity,
    0
  )

  const subtotal = cartItems.reduce(
    (sum, product) => sum + product.price * cart[product.id],
    0
  )

  const delivery = subtotal === 0 || subtotal >= 2000 ? 0 : 99
  const total = subtotal + delivery

  function addToCart(product) {
    const quantity = cart[product.id] || 0

    if (quantity >= product.stockQuantity) {
      setNotice(`Only ${product.stockQuantity} units of ${product.name} are available.`)
      return
    }

    setCart(previous => ({
      ...previous,
      [product.id]: (previous[product.id] || 0) + 1,
    }))

    setNotice(`${product.name} added to cart`)
  }

  function changeQuantity(productId, change) {
    const product = products.find(item => item.id === productId)
    if (!product) return

    setCart(previous => {
      const nextQuantity = (previous[productId] || 0) + change

      if (nextQuantity <= 0) {
        const next = { ...previous }
        delete next[productId]
        return next
      }

      if (nextQuantity > product.stockQuantity) {
        setNotice(`Only ${product.stockQuantity} units are available.`)
        return previous
      }

      return { ...previous, [productId]: nextQuantity }
    })
  }

  async function checkout() {
    if (!cartItems.length || checkingOut) return

    setCheckingOut(true)
    setNotice('')

    const createdOrders = []

    try {
      for (const product of cartItems) {
        const quantity = cart[product.id]

        const order = {
          userId: USER_ID,
          productId: product.id,
          quantity,
          totalPrice: Number((product.price * quantity).toFixed(2)),
          status: 'PENDING',
          createdAt: new Date().toISOString().slice(0, 19),
        }

        const response = await fetch(`${API}/orders`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(order),
        })

        if (!response.ok) {
          throw new Error(
            `Order for ${product.name} failed (HTTP ${response.status}).`
          )
        }

        createdOrders.push(await response.json())
      }

      setCart({})
      setShowCart(false)
      setNotice(
        `Successfully submitted ${createdOrders.length} order(s). Order processing is underway.`
      )
    } catch (error) {
      setNotice(
        `${error.message} ${createdOrders.length} order(s) were already submitted. Check order history before retrying.`
      )
    } finally {
      setCheckingOut(false)
    }
  }

  return (
    <div className="app-shell">
      <div className="announcement">
        Free delivery on orders over ₹2,000
      </div>

      <header className="site-header">
        <a className="brand" href="#" onClick={() => setShowCart(false)}>
          <span className="brand-mark">R</span>
          Rush<span>Cart</span>
        </a>

        <label className="search-box">
          <span>⌕</span>
          <input
            value={search}
            onChange={event => setSearch(event.target.value)}
            placeholder="Search products..."
            aria-label="Search products"
          />
          <kbd>Search</kbd>
        </label>

        <button className="cart-button" onClick={() => setShowCart(!showCart)}>
          <span>♧</span> Cart <span className="cart-count">{cartCount}</span>
        </button>
      </header>

      <main>
        {showCart ? (
          <section className="cart-page">
            <button className="back-link" onClick={() => setShowCart(false)}>
              ← Continue shopping
            </button>

            <div className="section-heading">
              <div>
                <span className="eyebrow">YOUR SELECTION</span>
                <h2>Your <span>cart</span></h2>
              </div>
            </div>

            {cartItems.length === 0 ? (
              <div className="empty-cart">
                <h2>Your cart is waiting</h2>
                <p>Add something you love from our collection.</p>
                <button className="primary-button" onClick={() => setShowCart(false)}>
                  Explore products
                </button>
              </div>
            ) : (
              <div className="cart-layout">
                <div className="cart-list">
                  {cartItems.map(product => (
                    <article className="cart-item" key={product.id}>
                      <div
                        className="product-art cart-art"
                        style={{ background: product.color }}
                      >
                        {product.icon}
                      </div>

                      <div className="cart-item-info">
                        <h3>{product.name}</h3>
                        <p>{product.description}</p>
                        <small>Stock available: {product.stockQuantity}</small>
                        <strong>{money(product.price)}</strong>
                      </div>

                      <div className="quantity-control">
                        <button onClick={() => changeQuantity(product.id, -1)}>
                          −
                        </button>
                        <span>{cart[product.id]}</span>
                        <button onClick={() => changeQuantity(product.id, 1)}>
                          +
                        </button>
                      </div>
                    </article>
                  ))}
                </div>

                               <aside className="order-summary">
                  <h2>Order summary</h2>

                  <div>
                    <span>Subtotal</span>
                    <strong>{money(subtotal)}</strong>
                  </div>

                  <div>
                    <span>Delivery</span>
                    <strong>{delivery === 0 ? 'Free' : money(delivery)}</strong>
                  </div>

                  <div className="summary-total">
                    <span>Total</span>
                    <strong>{money(total)}</strong>
                  </div>

                  <button
                    className="primary-button full-button"
                    onClick={checkout}
                    disabled={checkingOut || cartItems.length === 0}
                  >
                    {checkingOut ? 'Processing...' : 'Checkout'}
                  </button>

                  <p className="secure-note">
                    Secure checkout · Order updates processed through our backend
                  </p>
                </aside>
              </div>
            )}
          </section>
        ) : (
          <>
            <section className="hero">
              <div className="hero-copy">
                <span className="eyebrow">
                  <span className="eyebrow-dot" /> THE EVERYDAY EDIT
                </span>
                <h1>Good things.<br />Better <span>living.</span></h1>
                <p>
                  Thoughtfully picked essentials for your everyday life.
                  Discover your next favourite thing.
                </p>
                <a className="hero-cta" href="#products">
                  Shop the collection <span>↗</span>
                </a>
                <div className="hero-proof">
                  <span>✓ Curated picks</span>
                  <span>✓ Easy shopping</span>
                  <span>✓ Secure orders</span>
                </div>
              </div>

              <div className="hero-visual">
                <div className="hero-circle" />
                <div className="hero-product hero-product-one">💻</div>
                <div className="hero-product hero-product-two">🖱️</div>
                <div className="hero-product hero-product-three">🛍️</div>
                <div className="floating-label">✦ YOUR NEXT FAVOURITE</div>
                <div className="hero-sticker">Made for<br />everyday ↗</div>
              </div>
            </section>

            <section className="benefit-strip">
              <div><span>♧</span><section><strong>Free delivery</strong><small>On orders over ₹2,000</small></section></div>
              <div><span>↺</span><section><strong>Easy returns</strong><small>Hassle-free process</small></section></div>
              <div><span>♡</span><section><strong>Quality picks</strong><small>Selected for you</small></section></div>
              <div><span>◷</span><section><strong>Safe processing</strong><small>Microservice workflow</small></section></div>
            </section>

            <section className="catalog-section" id="products">
              <div className="section-heading">
                <div>
                  <span className="eyebrow">THE COLLECTION</span>
                  <h2>Find your <span>everyday</span></h2>
                  <p>Useful things. Thoughtful details. A little joy in every order.</p>
                </div>
                <span className="product-count">
                  {filteredProducts.length} products
                </span>
              </div>

              <div className="catalog-controls">
                <div className="category-tabs">
                  {categories.map(item => (
                    <button
                      key={item}
                      className={category === item ? 'category-tab active' : 'category-tab'}
                      onClick={() => setCategory(item)}
                    >
                      {item}
                    </button>
                  ))}
                </div>
                <span className="sort-label">Live catalog ↓</span>
              </div>

              {loading ? (
                <p>Loading products from the backend...</p>
              ) : (
                <div className="product-grid">
                  {filteredProducts.map(product => (
                    <article className="product-card" key={product.id}>
                      <div
                        className="product-art"
                        style={{ background: product.color }}
                      >
                        <span className="product-category">{product.category}</span>
                        <span className="product-emoji">{product.icon}</span>
                        <button
                          className="quick-add"
                          onClick={() => addToCart(product)}
                          disabled={product.stockQuantity <= 0}
                          aria-label={`Add ${product.name} to cart`}
                        >
                          +
                        </button>
                      </div>

                      <div className="product-info">
                        <div className="product-name-row">
                          <h3>{product.name}</h3>
                        </div>
                        <p>{product.description}</p>
                        <small>
                          {product.stockQuantity > 0
                            ? `${product.stockQuantity} in stock`
                            : 'Out of stock'}
                        </small>
                        <div className="product-price-row">
                          <strong>{money(product.price)}</strong>
                          <button
                            className="add-button"
                            onClick={() => addToCart(product)}
                            disabled={product.stockQuantity <= 0}
                          >
                            {product.stockQuantity > 0 ? 'Add to cart ↗' : 'Sold out'}
                          </button>
                        </div>
                      </div>
                    </article>
                  ))}

                  {filteredProducts.length === 0 && (
                    <p className="no-results">
                      No products found. Try another search.
                    </p>
                  )}
                </div>
              )}
            </section>
          </>
        )}
      </main>

      <footer className="site-footer">
        <a className="brand footer-brand" href="#">
          <span className="brand-mark">R</span> Rush<span>Cart</span>
        </a>
        <p>Everyday essentials, thoughtfully chosen.</p>
        <span>Built with a distributed microservices architecture.</span>
      </footer>

      {notice && (
        <div className="toast" role="status">
          {notice}
          <button onClick={() => setNotice('')} aria-label="Dismiss message"> × </button>
        </div>
      )}
    </div>
  )
}

export default App
