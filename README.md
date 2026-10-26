🏗️ BlockData - Public Work Escrow Platform (v1.0.0)
Plataforma GovTech SaaS B2B2C para la trazabilidad inmutable, gestión por hitos y liquidación automatizada de fondos en licitaciones de obra pública mediante Smart Contracts (Ethereum/EVM), IPFS y la Stablecoin USDT.

📌 Descripción General
El sistema automatiza todo el ciclo de vida de una obra pública adjudicada:

Licitación y Adjudicación: Postulación de contratistas y asignación transparente de pliegos.

Ejecución y Verificación por Hitos: Avance de obra respaldado por documentación documental descentralizada en IPFS.

Liberación Inmutable de Fondos: La aprobación de hitos desencadena transferencias automatizadas de USDT directamente desde el Smart Contract a la billetera del contratista.

Auditoría Final: Al completar el 100% de los hitos, el sistema emite un reporte consolidado respaldado en la blockchain.

🛠️ Stack Tecnológico
Smart Contracts: Solidity (^0.8.0), OpenZeppelin (ERC-20 USDT), Hardhat / Anvil.

Backend: Java 17, Spring Boot, Spring Data JPA, Web3j, MySQL.

Almacenamiento Descentralizado: IPFS (Kubo / Pinata API).

Frontend: JavaScript ES6+, HTML5, CSS3, Bootstrap 5.

📐 Arquitectura de Datos y Sincronización
La blockchain es la única fuente de verdad (Single Source of Truth) para los estados de ejecución y pagos. La base de datos MySQL actúa como índice relacional rápido y disparador de workflows administrativos.

🚀 Guía de Instalación y Configuración Local
Prerrequisitos
Java JDK 17 o superior.

Maven 3.8+.

MySQL Server 8.0+.

Node.js & npm (para la red blockchain local).

1. Despliegue de la Red Blockchain Local
En tu consola de entorno de contratos (Hardhat / Ganache / Anvil):

# Iniciar nodo local
npx hardhat node

# Compilar y desplegar Smart Contracts
npx hardhat run scripts/deploy.js --network localhost

2. Configuración del Backend (Spring Boot)
 server:
  port: 8082

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/public_work_escrow_db?createDatabaseIfNotExist=true
    username: root
    password: tu_password

blockchain:
  rpc-url: http://127.0.0.1:8545
  private-key-estado: 0x... # Clave privada del Estado / Comitente

  
  
  mvn clean install
  
  mvn spring-boot:run

3. Ejecución del Frontend
Abre el archivo index.html o sirve la carpeta static/ con Live Server o tu servidor estático preferido:

npx live-server --port=5500

🧪 Flujo de Prueba de Cierre de Obra (Smoke Test)
Iniciar Sesión: Ingresa con credenciales de CONTRACTOR en el panel web.

Subir Evidencia: En Obras Asignadas, selecciona un hito y sube un archivo en PDF/Doc. El sistema lo subirá a IPFS y enviará la solicitud a revisión.

Aprobación Estatal: Desde el panel de licitante/Estado, presiona Aprobar Hito. Se ejecutará la transacción aprobarYLiberarHito en la blockchain, transfiriendo los USDT correspondientes al contratista.

Verificación visual: Al actualizar el listado de hitos, el hash IPFS se renderizará automáticamente en la tabla.

Cierre de Obra: Al aprobar el 100% de los hitos, aparecerá la tarjeta de 🎉 ¡Obra Completada al 100%! con el botón para generar el Certificado de Auditoría y Resumen Final.


## 📄 Endpoints Principales del Sistema (v1.0.0)

### 1. Gestión de Licitaciones y Pliegos (`/api/pliegos`)
* **`POST` `/api/pliegos/upload`**: Sube el pliego base de la obra a IPFS (Docker local), registra los hitos iniciales en MySQL y despliega el Smart Contract en la blockchain EVM.
* **`GET` `/api/pliegos/all`**: Lista todas las licitaciones públicas registradas en el sistema.
* **`GET` `/api/pliegos/{id}`**: Obtiene el detalle de una licitación específica por su ID relacional.
* **`GET` `/api/pliegos/contratista/{contractorId}/asignadas`**: Devuelve las obras adjudicadas y activas asignadas a un contratista específico.

### 2. Ejecución y Auditoría On-Chain (`/api/licitaciones`)
* **`GET` `/api/licitaciones/{contractAddress}/listaHitos`**: Consulta y lee la lista completa de hitos directamente desde el Smart Contract (Single Source of Truth).
* **`POST` `/api/licitaciones/{contractAddress}/ofertas`**: Permite a una empresa contratista postularse a una licitación subiendo su propuesta a IPFS.
* **`GET` `/api/licitaciones/{contractAddress}/ofertas`**: Consulta la lista de ofertas presentadas para un contrato específico.
* **`POST` `/api/licitaciones/{contractAddress}/adjudicar`**: Ejecuta la adjudicación ganadora en el contrato inteligente, aprueba el gasto de USDT en custodia y deposita los fondos iniciales.
* **`POST` `/api/licitaciones/{hitoIndex}/subir-evidencia`**: Sube el documento de avance de un hito a IPFS (Docker) y actualiza el estado local a `EN_REVISION`.
* **`POST` `/api/licitaciones/{hitoId}/aprobar-y-liberar`**: Ejecuta la transacción firmada por el Estado para aprobar el hito en el contrato inteligente y liberar automáticamente los fondos en USDT al contratista.
* **`GET` `/api/licitaciones/{contractAddress}/resumen-final`**: Genera y devuelve el reporte consolidado de cierre de obra, métricas de avance y la auditoría inmutable al alcanzar el 100% de aprobación.y la auditoría inmutable al alcanzar el 100% de aprobación.
