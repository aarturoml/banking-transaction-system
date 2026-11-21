# Ledgerly frontend

Frontend Angular para el backend bancario simulado.

## Ejecutar en desarrollo

Desde esta carpeta:

```bash
npm install
npm start
```

Abre `http://localhost:4200`.

El proxy de desarrollo (`proxy.conf.json`) redirige `/api` al backend local:

```text
http://localhost:8080
```

Con Docker Compose, el frontend queda disponible en `http://localhost:4200` y
Nginx redirige `/api` directamente al servicio `app`.

## Funcionalidades

- Registro e inicio de sesión con JWT.
- Interceptor HTTP que añade automáticamente el token.
- Protección de rutas autenticadas.
- Creación de cuentas USD, EUR y GBP.
- Visualización de saldos.
- Depósitos simulados para administradores.
- Transferencias idempotentes.
- Historial de movimientos y ledger.

## Build

```bash
npm run build
```
