import hre from "hardhat";

async function main() {
  const [deployer] = await hre.ethers.getSigners();
  console.log("Desplegando MockUSDT con la cuenta:", deployer.address);

  const MockUSDT = await hre.ethers.getContractFactory("MockUSDT");
  const usdt = await MockUSDT.deploy();

  await usdt.waitForDeployment();

  const usdtAddress = await usdt.getAddress();
  console.log("----------------------------------------");
  console.log("MockUSDT desplegado exitosamente en:", usdtAddress);
  console.log("----------------------------------------");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});