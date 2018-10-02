package recommendation;

public class UserProductAutoencoder {

    public static void main(String[] args) throws Exception {
        //Neural net configuration
        Nd4j.getRandom().setSeed(rngSeed);
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(rngSeed)
                .updater(new RmsProp(1e-2))
                .weightInit(WeightInit.XAVIER)
                .l2(1e-4)
                .list()
                .layer(0, new VariationalAutoencoder.Builder()
                        .activation(Activation.LEAKYRELU)
                        .encoderLayerSizes(256, 256)        //2 encoder layers, each of size 256
                        .decoderLayerSizes(256, 256)        //2 decoder layers, each of size 256
                        .pzxActivationFunction(Activation.IDENTITY)  //p(z|data) activation function
                        .reconstructionDistribution(new BernoulliReconstructionDistribution(Activation.SIGMOID.getActivationFunction()))     //Bernoulli distribution for p(data|z) (binary or 0 to 1 data only)
                        .nIn(28 * 28)                       //Input size: 28x28
                        .nOut(2)                            //Size of the latent variable space: p(z|x). 2 dimensions here for plotting, use more in general
                        .build())
                .pretrain(true).backprop(false).build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        //Get the variational autoencoder layer
        org.deeplearning4j.nn.layers.variational.VariationalAutoencoder vae
                = (org.deeplearning4j.nn.layers.variational.VariationalAutoencoder) net.getLayer(0);


        //Test data for plotting
        DataSet testdata = new MnistDataSetIterator(10000, false, rngSeed).next();
        INDArray testFeatures = testdata.getFeatures();
        INDArray testLabels = testdata.getLabels();
        INDArray latentSpaceGrid = getLatentSpaceGrid(plotMin, plotMax, plotNumSteps);              //X/Y grid values, between plotMin and plotMax

        //Lists to store data for later plotting
        List<INDArray> latentSpaceVsEpoch = new ArrayList<>(nEpochs + 1);
        INDArray latentSpaceValues = vae.activate(testFeatures, false, LayerWorkspaceMgr.noWorkspaces());                     //Collect and record the latent space values before training starts
        latentSpaceVsEpoch.add(latentSpaceValues);
        List<INDArray> digitsGrid = new ArrayList<>();


        //Add a listener to the network that, every N=100 minibatches:
        // (a) collect the test set latent space values for later plotting
        // (b) collect the reconstructions at each point in the grid
        net.setListeners(new PlottingListener(100, testFeatures, latentSpaceGrid, latentSpaceVsEpoch, digitsGrid));

        //Perform training
        for (int i = 0; i < nEpochs; i++) {
            log.info("Starting epoch {} of {}",(i+1),nEpochs);
            net.pretrain(trainIter);    //Note use of .pretrain(DataSetIterator) not fit(DataSetIterator) for unsupervised training
        }
    }
}
