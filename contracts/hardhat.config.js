import "@nomicfoundation/hardhat-toolbox";

/** @type import('hardhat/config').HardhatUserConfig */
const config = {
  solidity: "0.8.20",
  networks: {
    hardhat: {
      chainId: 31337
    }
  }
};

export default config;