// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

// Interfaz minima del estándar ERC-20 para interactuar con USDT
interface IERC20 {
    function transfer(address recipient, uint256 amount) external returns (bool);
    function transferFrom(address sender, address recipient, uint256 amount) external returns (bool);
    function balanceOf(address account) external view returns (uint256);
}

contract PublicWorkEscrow {

    enum State { Publicada, Adjudicada, EnEjecucion, Finalizada, Cancelada }

    

    struct MilestoneInput {
        string descripcion;
        uint256 porcentaje; // Porcentaje entero (ej: 20 para 20%)
    }

    struct DatosGanador {
        address billeteraContratista;
        string ipfsPropuestaHash;
        uint256 montoAdjudicado; // Monto en unidades de USDT (ej: 100 USDT = 100_000_000)
        uint256 fechaAdjudicacion;
    }

    struct Milestone {
        string descripcion;
        uint256 porcentaje;
        bool completado;
        bool aprobado;
    }

    struct Oferta {
        address contratista;
        uint256 montoTotal;
        string hashPropuestaHash;
        uint256 timestamp;
    }

    IERC20 public immutable usdtToken; // Dirección del contrato del Token USDT
    address public licitante;
    string public ipfsPliegoHash;
    State public estadoActual;
    
    DatosGanador public ganador;
    Milestone[] public hitos;
    uint256 public montoTotal;
    Oferta[] public ofertas;   

   

    event LicitacionPublicada(string ipfsPliegoHash, address indexed licitante);
    event ObraAdjudicada(address indexed contratista, uint256 monto, string ipfsPropuestaHash);
    event HitoCompletado(uint256 indexHito);
    event HitoAprobadoYPagado(uint256 indexHito, uint256 montoLiberado, uint256 porcentaje);
    // Evento que se emite cuando una empresa registra su oferta exitosamente
    event OfertaPresentada(address indexed contratista, uint256 montoTotal, string hashPropuesta);

    // Mapping para evitar que una misma empresa envíe más de una oferta por licitación
    mapping(address => bool) public yaOferto;

    modifier soloLicitante() {
        require(msg.sender == licitante, "Solo la entidad licitante puede ejecutar esto");
        _;
    }

    modifier soloAdjudicatario() {
        require(msg.sender == ganador.billeteraContratista, "Solo el contratista puede ejecutar esto");
        _;
    }

    modifier enEstado(State _estado) {
        require(estadoActual == _estado, "Estado invalido para esta accion");
        _;
    }

    // El constructor ahora recibe la direccion del token USDT
    constructor(
        address _usdtTokenAddress,
        string memory _ipfsPliegoHash, 
        MilestoneInput[] memory _hitosIniciales
    ) {
        require(_usdtTokenAddress != address(0), "Direccion USDT invalida");
        
        usdtToken = IERC20(_usdtTokenAddress);
        licitante = msg.sender;
        ipfsPliegoHash = _ipfsPliegoHash;
        estadoActual = State.Publicada;

        uint256 acumuladoPorcentaje = 0;

        for (uint256 i = 0; i < _hitosIniciales.length; i++) {
            require(_hitosIniciales[i].porcentaje > 0, "El porcentaje debe ser mayor a cero");
            acumuladoPorcentaje += _hitosIniciales[i].porcentaje;

            hitos.push(Milestone({
                descripcion: _hitosIniciales[i].descripcion,
                porcentaje: _hitosIniciales[i].porcentaje,
                completado: false,
                aprobado: false
            }));
        }

        require(acumuladoPorcentaje <= 100, "La suma de porcentajes no puede superar el 100%");

        emit LicitacionPublicada(_ipfsPliegoHash, msg.sender);
    }

    function obtenerCantidadHitos() external view returns (uint256) {
        return hitos.length;
    }

    function adjudicarGanador(
        address _billeteraContratista, 
        string memory _ipfsPropuestaHash,
        uint256 _montoAdjudicado
    ) external soloLicitante enEstado(State.Publicada) {
        require(_billeteraContratista != address(0), "Direccion invalida");
        require(_montoAdjudicado > 0, "El monto adjudicado debe ser mayor a cero");

        ganador = DatosGanador({
            billeteraContratista: _billeteraContratista,
            ipfsPropuestaHash: _ipfsPropuestaHash,
            montoAdjudicado: _montoAdjudicado,
            fechaAdjudicacion: block.timestamp
        });

        estadoActual = State.Adjudicada;

        emit ObraAdjudicada(_billeteraContratista, _montoAdjudicado, _ipfsPropuestaHash);
    }

  
    function depositarFondosYComenzar() external soloLicitante enEstado(State.Adjudicada) {
        uint256 montoRequerido = ganador.montoAdjudicado;
        
        montoTotal = montoRequerido;
        estadoActual = State.EnEjecucion;

        // Transfiere los USDT desde la billetera del Licitante hacia este Escrow
        // (El licitante debe haber ejecutado usdt.approve(address(this), montoRequerido) antes)
        bool success = usdtToken.transferFrom(msg.sender, address(this), montoRequerido);
        require(success, "Fallo el deposito de USDT");
    }

    function agregarHito(string memory _descripcion, uint256 _porcentaje) external soloLicitante {
        require(_porcentaje > 0, "El porcentaje debe ser mayor a cero");
        
        uint256 sumaPorcentajes = 0;
        for (uint256 i = 0; i < hitos.length; i++) {
            sumaPorcentajes += hitos[i].porcentaje;
        }
        require(sumaPorcentajes + _porcentaje <= 100, "La suma total de porcentajes no puede superar el 100%");

        hitos.push(Milestone(_descripcion, _porcentaje, false, false));
    }

    function marcarHitoCompletado(uint256 _indexHito) external soloAdjudicatario enEstado(State.EnEjecucion) {
        require(_indexHito < hitos.length, "Hito inexistente");
        hitos[_indexHito].completado = true;

        emit HitoCompletado(_indexHito);
    }

    function aprobarYLiberarHito(uint256 _indexHito) external soloLicitante enEstado(State.EnEjecucion) {
        require(_indexHito < hitos.length, "Hito inexistente");
        Milestone storage hito = hitos[_indexHito];
        
        require(hito.completado, "El hito no fue marcado como completado");
        require(!hito.aprobado, "El hito ya fue pagado");

        // Calculo del monto en USDT a liberar según el porcentaje del monto adjudicado
        uint256 montoAPagar = (ganador.montoAdjudicado * hito.porcentaje) / 100;

        require(usdtToken.balanceOf(address(this)) >= montoAPagar, "Fondos en USDT insuficientes en el contrato");

        hito.aprobado = true;
        
        // Transfiere los tokens USDT directamente al contratista
        bool success = usdtToken.transfer(ganador.billeteraContratista != address(0) ? ganador.billeteraContratista : msg.sender, montoAPagar);
        require(success, "Fallo la transferencia de USDT al contratista");

        emit HitoAprobadoYPagado(_indexHito, montoAPagar, hito.porcentaje);
    }

    function obtenerTodosLosHitos() external view returns  (Milestone[] memory){

        return hitos;
    }

        /**
     * @notice Permite a una empresa contratista enviar su oferta
     * @param _montoTotal Monto presupuestado por la empresa
     * @param _hashPropuesta Hash IPFS del documento de la propuesta
     */
    function presentarOferta(uint256 _montoTotal, string memory _hashPropuesta) external {
        require(!yaOferto[msg.sender], "La empresa ya ha presentado una oferta");
        require(_montoTotal > 0, "El monto ofertado debe ser mayor a cero");

        ofertas.push(Oferta({
            contratista: msg.sender,
            montoTotal: _montoTotal,
            hashPropuestaHash: _hashPropuesta,
            timestamp: block.timestamp
        }));

        yaOferto[msg.sender] = true;

        emit OfertaPresentada(msg.sender, _montoTotal, _hashPropuesta);
    }

    /**
     * @notice Devuelve la cantidad total de ofertas recibidas
     */
    function obtenerCantidadOfertas() external view returns (uint256) {
        return ofertas.length;
    }

    /**
     * @notice Devuelve el arreglo completo de ofertas para lectura desde Web3j
     */
    function obtenerTodasLasOfertas() external view returns (Oferta[] memory) {
        return ofertas;
    }



     
}