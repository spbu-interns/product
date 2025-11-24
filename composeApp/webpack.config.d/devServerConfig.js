config.devServer = {
  ...config.devServer, // аккуратно мёржим с существующей конфигурацией
  historyApiFallback: true
};