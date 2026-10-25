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
        uint256 porcentaje; 
    }

    struct DatosGanador {
        address billeteraContratista;
        string ipfsPropuestaHash;
        uint256 montoAdjudicado; 
        uint256 fechaAdjudicacion;
    }

    struct Milestone {
        string descripcion;
        uint256 porcentaje;
        bool completado;
        bool aprobado;
        string ipfsHash;
    }

    struct Oferta {
        address contratista;
        uint256 montoTotal;
        string hashPropuestaHash;
        uint256 timestamp;
    }

    IERC20 public immutable usdtToken;
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
    event ObraCompletada(address indexed obraAddress, uint256 timestamp);

    
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
                aprobado: false,
                ipfsHash: ""
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

        
        hitos.push(Milestone({
            descripcion: _descripcion,
            porcentaje: _porcentaje,
            completado: false,
            aprobado: false,
            ipfsHash: ""
        }));
    }

    function marcarHitoCompletado(uint256 _indexHito, string memory _ipfsHash) external soloAdjudicatario enEstado(State.EnEjecucion) {
        require(_indexHito < hitos.length, "Hito inexistente");
        hitos[_indexHito].completado = true;
        hitos[_indexHito].ipfsHash = _ipfsHash;

        emit HitoCompletado(_indexHito);
    }

        function aprobarYLiberarHito(uint256 _indexHito, string memory _ipfsHash) external soloLicitante enEstado(State.EnEjecucion) {
                require(_indexHito < hitos.length, "Hito inexistente");
                Milestone storage hito = hitos[_indexHito];
                
                
                hito.completado = true; 
                
                require(!hito.aprobado, "El hito ya fue pagado");

                
                hito.ipfsHash = _ipfsHash;

                
                uint256 montoAPagar = (ganador.montoAdjudicado * hito.porcentaje) / 100;
                require(usdtToken.balanceOf(address(this)) >= montoAPagar, "Fondos en USDT insuficientes en el contrato");

                hito.aprobado = true;
                
                bool success = usdtToken.transfer(ganador.billeteraContratista != address(0) ? ganador.billeteraContratista : msg.sender, montoAPagar);
                require(success, "Fallo la transferencia de USDT al contratista");

                emit HitoAprobadoYPagado(_indexHito, montoAPagar, hito.porcentaje);


                bool todosAprobados = true;
                    for (uint i = 0; i < hitos.length; i++) {
                        if (!hitos[i].aprobado) {
                            todosAprobados = false;
                            break;  
                        }
                    }

                    if (todosAprobados) {
                        estadoActual = State.Finalizada; 
                        emit ObraCompletada(address(this), block.timestamp);
                    }
            }

    function obtenerTodosLosHitos() external view returns  (Milestone[] memory){

        return hitos;
    }

  
    function presentarOferta(uint256 _montoTotal, string memory _hashPropuesta) external {
   
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

   
    function obtenerCantidadOfertas() external view returns (uint256) {
        return ofertas.length;
    }


    function obtenerTodasLasOfertas() external view returns (Oferta[] memory) {
        return ofertas;
    }


    
    function getHito(uint256 index) public view returns (
        string memory descripcion, 
        uint256 porcentaje, 
        bool completado, 
        bool aprobado, 
        string memory ipfsHash
    ) {
        require(index < hitos.length, "Indice de hito fuera de rango");
        
        Milestone memory h = hitos[index]; 
        
        return (h.descripcion, h.porcentaje, h.completado, h.aprobado, h.ipfsHash);
    }
     
}