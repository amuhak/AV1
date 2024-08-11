const path = require('path');
const CssMin = require('css-minimizer-webpack-plugin');
const TerserPlugin = require("terser-webpack-plugin");
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const HtmlMinimizerPlugin = require('html-minimizer-webpack-plugin');

module.exports = {
    mode: 'production', // or 'development' or 'none'
    entry: {
        index: './src/index.js',
    },
    output: {
        path: path.resolve(__dirname + '/../resources/templates'),
        filename: '[name].bundle.js',
    },
    module: {
        rules: [
            {
                test: /\.css$/i,
                use: [MiniCssExtractPlugin.loader, 'css-loader'],
            },
        ],
    },
    plugins: [
        new MiniCssExtractPlugin(),
        new HtmlWebpackPlugin({
            filename: 'index.html',
            template: './src/index.html',
            chunks: ['index'],
            minify: true,
        }),
    ],
    optimization: {
        minimize: true,
        minimizer: [
            new TerserPlugin(),
            new CssMin(),
            new HtmlMinimizerPlugin(),
        ],
    },
};
