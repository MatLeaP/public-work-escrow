// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract MockUSDT is ERC20 {
    constructor() ERC20("Tether USD", "USDT") {
        // Le acuñamos 1,000,000 de USDT a la cuenta desplegadora
        // Recordar que USDT usa 6 decimales (1,000,000 * 10^6)
        _mint(msg.sender, 1_000_000 * 10**6);
    }

    function decimals() public pure override returns (uint8) {
        return 6;
    }

    // Función auxiliar para que cualquier cuenta de prueba pueda pedir USDT gratis
    function mint(address to, uint256 amount) external {
        _mint(to, amount);
    }
}