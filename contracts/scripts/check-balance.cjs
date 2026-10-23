const { ethers } = require("hardhat");

async function main() {
  const contractAddress = "0xe7f1725e7734ce288f8367e1bb143e90bb3f0512"; // Dirección de tu PublicWorkEscrow
  const usdtAddress = "0x5FbDB2315678afecb367f032d93F642f64180aa3";     // Dirección de tu MockUSDT

  // Instanciar el contrato de MockUSDT usando su ABI estándar de ERC20
  const mockUSDT = await ethers.getContractAt("MockUSDT", usdtAddress);

  // Consultar el balance de USDT que posee el contrato Escrow
  const balance = await mockUSDT.balanceOf(contractAddress);
  
  // Como MockUSDT tiene 6 decimales, formateamos el número
  console.log(`Balance de USDT en el contrato Escrow: ${ethers.formatUnits(balance, 6)} USDT`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});