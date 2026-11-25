# Diccionario de datos: Medical Printer (PostgreSQL 16)

Las columnas coinciden con los campos del `openapi.yaml`. Donde el mapeo dice "campo simple", el nombre se deriva del atributo en snake_case.

## empresas

| Columna | Tipo SQL | Restricciones | Descripción | Mapeo JPA |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PK | Identificador | `@Id @GeneratedValue(IDENTITY)` |
| nombre | VARCHAR(150) | NOT NULL | Razón social | Campo simple |
| nit | VARCHAR(20) | NOT NULL, UNIQUE | NIT; una empresa se registra una sola vez | `@Column(nullable=false, unique=true, length=20)` |
| telefono | VARCHAR(20) | NULL | Teléfono de contacto | Campo simple |
| correo | VARCHAR(150) | NULL | Correo de la empresa | Campo simple |
| direccion | VARCHAR(200) | NULL | Dirección | Campo simple |
| ciudad | VARCHAR(100) | NULL | Ciudad | Campo simple |
| activa | BOOLEAN | NOT NULL, DEFAULT TRUE | Empresa habilitada | Campo simple |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() | Fecha de registro | Campo `createdAt` |

## usuarios

| Columna | Tipo SQL | Restricciones | Descripción | Mapeo JPA |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PK | Identificador | `@Id @GeneratedValue(IDENTITY)` |
| empresa_id | BIGINT | FK a empresas(id), NULL | Empresa a la que pertenece | `@ManyToOne(fetch=LAZY) @JoinColumn` |
| nombre | VARCHAR(120) | NOT NULL | Nombre completo | Campo simple |
| correo | VARCHAR(150) | NOT NULL, UNIQUE | Correo de acceso en minúsculas (RN-02) | `@Column(nullable=false, unique=true, length=150)` |
| password_hash | VARCHAR(100) | NOT NULL | Contraseña cifrada con BCrypt | Campo `passwordHash` |
| telefono | VARCHAR(20) | NULL | Teléfono | Campo simple |
| rol | VARCHAR(10) | NOT NULL, DEFAULT 'USUARIO', CHECK IN ('ADMIN','USUARIO') | Rol del usuario (RN-06) | `@Enumerated(EnumType.STRING)` |
| activo | BOOLEAN | NOT NULL, DEFAULT TRUE | Cuenta habilitada | Campo simple |
| intentos_fallidos | INT | NOT NULL, DEFAULT 0 | Contador para el bloqueo (RNF-03) | Campo `intentosFallidos` |
| bloqueado_hasta | TIMESTAMP | NULL | Fin del bloqueo de 15 minutos | Campo `bloqueadoHasta` |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() | Fecha de registro | Campo `createdAt` |

## categorias

| Columna | Tipo SQL | Restricciones | Descripción | Mapeo JPA |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PK | Identificador | `@Id @GeneratedValue(IDENTITY)` |
| nombre | VARCHAR(100) | NOT NULL, UNIQUE | Especialidad | Campo simple |

## productos

| Columna | Tipo SQL | Restricciones | Descripción | Mapeo JPA |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PK | Identificador | `@Id @GeneratedValue(IDENTITY)` |
| categoria_id | BIGINT | NOT NULL, FK a categorias(id) | Categoría del equipo | `@ManyToOne(optional=false, fetch=LAZY)` |
| nombre | VARCHAR(150) | NOT NULL | Nombre comercial | Campo simple |
| descripcion | TEXT | NULL | Especificaciones | `@Column(columnDefinition="text")` |
| imagen_url | VARCHAR(300) | NULL | Imagen del producto | Campo `imagenUrl` |
| precio | NUMERIC(12,2) | NOT NULL, CHECK (precio >= 0) | Precio vigente en COP | `@Column(nullable=false, precision=12, scale=2)` |
| stock | INT | NOT NULL, DEFAULT 0, CHECK (stock >= 0) | Unidades disponibles (RNF-02) | Campo simple |
| estado | VARCHAR(15) | NOT NULL, DEFAULT 'DISPONIBLE', CHECK IN ('DISPONIBLE','NO_DISPONIBLE') | Disponibilidad (RN-03) | `@Enumerated(EnumType.STRING)` |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() | Fecha de alta | Campo `createdAt` |

Índice: `idx_productos_estado_categoria (estado, categoria_id)` para el catálogo (RNF-01).

## pedidos

| Columna | Tipo SQL | Restricciones | Descripción | Mapeo JPA |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PK | Número de pedido | `@Id @GeneratedValue(IDENTITY)` |
| usuario_id | BIGINT | NOT NULL, FK a usuarios(id) | Quien realiza el pedido | `@ManyToOne(optional=false, fetch=LAZY)` |
| destinatario_tipo | VARCHAR(10) | NOT NULL, CHECK IN ('PERSONA','EMPRESA') | Tipo de destinatario (RN-04) | `@Enumerated(EnumType.STRING)` |
| empresa_id | BIGINT | FK a empresas(id), NULL | Empresa destinataria | `@ManyToOne(fetch=LAZY)` |
| contacto | VARCHAR(150) | NOT NULL | Teléfono o correo de contacto (RN-04) | Campo simple |
| estado | VARCHAR(12) | NOT NULL, DEFAULT 'PENDIENTE', CHECK IN (5 estados) | Estado del pedido (RN-05, RN-07) | `@Enumerated(EnumType.STRING)` |
| total | NUMERIC(12,2) | NOT NULL, DEFAULT 0 | Suma de ítems | Campo simple |
| idempotency_key | VARCHAR(64) | UNIQUE | Evita pedidos duplicados (RNF-05) | `@Column(unique=true, length=64)` |
| version | BIGINT | NOT NULL, DEFAULT 0 | Control de concurrencia entre administradores | `@Version` |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() | Fecha de creación | Campo `createdAt` |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT now() | Última modificación | Campo `updatedAt` |

Restricción de tabla: `CHECK ((destinatario_tipo='EMPRESA' AND empresa_id IS NOT NULL) OR (destinatario_tipo='PERSONA' AND empresa_id IS NULL))`.
Índice: `idx_pedidos_usuario (usuario_id, created_at)` para "Mis pedidos".

## pedido_items

| Columna | Tipo SQL | Restricciones | Descripción | Mapeo JPA |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PK | Identificador | `@Id @GeneratedValue(IDENTITY)` |
| pedido_id | BIGINT | NOT NULL, FK a pedidos(id) ON DELETE CASCADE | Pedido al que pertenece | `@ManyToOne(optional=false, fetch=LAZY)` |
| producto_id | BIGINT | NOT NULL, FK a productos(id) | Producto pedido | `@ManyToOne(optional=false, fetch=LAZY)` |
| cantidad | INT | NOT NULL, CHECK (cantidad >= 1) | Unidades pedidas | Campo simple |
| precio_unitario | NUMERIC(12,2) | NOT NULL | Precio congelado al crear el pedido (RN-10) | Campo `precioUnitario` |

Restricción: `UNIQUE (pedido_id, producto_id)`, mapeada con `@UniqueConstraint`.

## historial_estados

| Columna | Tipo SQL | Restricciones | Descripción | Mapeo JPA |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PK | Identificador | `@Id @GeneratedValue(IDENTITY)` |
| pedido_id | BIGINT | NOT NULL, FK a pedidos(id) | Pedido que cambió | `@ManyToOne(optional=false, fetch=LAZY)` |
| estado_anterior | VARCHAR(12) | NOT NULL | Estado de origen | `@Enumerated(EnumType.STRING)` |
| estado_nuevo | VARCHAR(12) | NOT NULL | Estado de destino | `@Enumerated(EnumType.STRING)` |
| cambiado_por | BIGINT | NOT NULL, FK a usuarios(id) | Administrador que hizo el cambio (RN-06) | `@ManyToOne(optional=false, fetch=LAZY)` |
| fecha | TIMESTAMP | NOT NULL, DEFAULT now() | Momento del cambio | Campo simple |

## facturas

| Columna | Tipo SQL | Restricciones | Descripción | Mapeo JPA |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PK | Identificador | `@Id @GeneratedValue(IDENTITY)` |
| pedido_id | BIGINT | NOT NULL, UNIQUE, FK a pedidos(id) | Un pedido solo tiene una factura (RN-09) | `@OneToOne(optional=false, fetch=LAZY)` |
| numero | BIGINT | NOT NULL, UNIQUE | Consecutivo sin repeticiones (RN-10) | `@Column(nullable=false, unique=true)` |
| subtotal | NUMERIC(12,2) | NOT NULL | Suma de precio por cantidad | Campo simple |
| impuestos | NUMERIC(12,2) | NOT NULL | IVA del 19 % | Campo simple |
| total | NUMERIC(12,2) | NOT NULL | Subtotal más impuestos | Campo simple |
| emitida_por | BIGINT | NOT NULL, FK a usuarios(id) | Administrador que factura | `@ManyToOne(optional=false, fetch=LAZY)` |
| fecha_emision | TIMESTAMP | NOT NULL, DEFAULT now() | Fecha de la factura | Campo `fechaEmision` |

## consecutivos

| Columna | Tipo SQL | Restricciones | Descripción | Mapeo JPA |
| --- | --- | --- | --- | --- |
| serie | VARCHAR(20) | PK | Serie de numeración, por ejemplo FACTURA | `@Id` |
| ultimo_numero | BIGINT | NOT NULL, DEFAULT 0 | Último número emitido; se lee con bloqueo (RNF-06) | Campo `ultimoNumero` |
