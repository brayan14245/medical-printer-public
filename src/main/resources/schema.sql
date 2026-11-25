CREATE TABLE IF NOT EXISTS empresas (
  id BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(150) NOT NULL,
  nit VARCHAR(20) NOT NULL UNIQUE,
  telefono VARCHAR(20),
  correo VARCHAR(150),
  direccion VARCHAR(200),
  ciudad VARCHAR(100),
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS usuarios (
  id BIGSERIAL PRIMARY KEY,
  empresa_id BIGINT REFERENCES empresas(id),
  nombre VARCHAR(120) NOT NULL,
  correo VARCHAR(150) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  telefono VARCHAR(20),
  rol VARCHAR(10) NOT NULL DEFAULT 'USUARIO' CHECK (rol IN ('ADMIN','USUARIO')),
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  intentos_fallidos INT NOT NULL DEFAULT 0,
  bloqueado_hasta TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS categorias (
  id BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS productos (
  id BIGSERIAL PRIMARY KEY,
  categoria_id BIGINT NOT NULL REFERENCES categorias(id),
  nombre VARCHAR(150) NOT NULL,
  descripcion TEXT,
  imagen_url VARCHAR(300),
  precio NUMERIC(12,2) NOT NULL CHECK (precio >= 0),
  stock INT NOT NULL DEFAULT 0 CHECK (stock >= 0),
  estado VARCHAR(15) NOT NULL DEFAULT 'DISPONIBLE',
  created_at TIMESTAMP NOT NULL DEFAULT now()
);
UPDATE productos SET estado = 'INACTIVO' WHERE estado = 'NO_DISPONIBLE';
ALTER TABLE productos DROP CONSTRAINT IF EXISTS productos_estado_check;
ALTER TABLE productos
  ADD CONSTRAINT productos_estado_check
  CHECK (estado IN ('DISPONIBLE','AGOTADO','INACTIVO'));
CREATE INDEX IF NOT EXISTS idx_productos_estado_categoria ON productos (estado, categoria_id);

CREATE TABLE IF NOT EXISTS pedidos (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
  destinatario_tipo VARCHAR(10) NOT NULL CHECK (destinatario_tipo IN ('PERSONA','EMPRESA')),
  empresa_id BIGINT REFERENCES empresas(id),
  contacto VARCHAR(150) NOT NULL,
  estado VARCHAR(12) NOT NULL DEFAULT 'PENDIENTE'
    CHECK (estado IN ('PENDIENTE','CONFIRMADO','ENVIADO','ENTREGADO','CANCELADO')),
  total NUMERIC(12,2) NOT NULL DEFAULT 0,
  idempotency_key VARCHAR(64) UNIQUE,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  CHECK ((destinatario_tipo = 'EMPRESA' AND empresa_id IS NOT NULL)
      OR (destinatario_tipo = 'PERSONA' AND empresa_id IS NULL))
);
CREATE INDEX IF NOT EXISTS idx_pedidos_usuario ON pedidos (usuario_id, created_at);

CREATE TABLE IF NOT EXISTS pedido_items (
  id BIGSERIAL PRIMARY KEY,
  pedido_id BIGINT NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
  producto_id BIGINT NOT NULL REFERENCES productos(id),
  cantidad INT NOT NULL CHECK (cantidad >= 1),
  precio_unitario NUMERIC(12,2) NOT NULL,
  UNIQUE (pedido_id, producto_id)
);

CREATE TABLE IF NOT EXISTS historial_estados (
  id BIGSERIAL PRIMARY KEY,
  pedido_id BIGINT NOT NULL REFERENCES pedidos(id),
  estado_anterior VARCHAR(12) NOT NULL,
  estado_nuevo VARCHAR(12) NOT NULL,
  cambiado_por BIGINT NOT NULL REFERENCES usuarios(id),
  fecha TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS facturas (
  id BIGSERIAL PRIMARY KEY,
  pedido_id BIGINT NOT NULL UNIQUE REFERENCES pedidos(id),
  numero BIGINT NOT NULL UNIQUE,
  subtotal NUMERIC(12,2) NOT NULL,
  impuestos NUMERIC(12,2) NOT NULL,
  total NUMERIC(12,2) NOT NULL,
  emitida_por BIGINT NOT NULL REFERENCES usuarios(id),
  fecha_emision TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS consecutivos (
  serie VARCHAR(20) PRIMARY KEY,
  ultimo_numero BIGINT NOT NULL DEFAULT 0
);
INSERT INTO consecutivos (serie, ultimo_numero) VALUES ('FACTURA', 0) ON CONFLICT (serie) DO NOTHING;
