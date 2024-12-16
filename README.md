# Social Media Sentiment Analysis Application

A comprehensive Java application that performs sentiment analysis on social media content using Apache Hadoop, Apache Spark, and various NLP technologies. The application features a modern JavaFX GUI interface and integrates with Reddit's API for data collection.

## Features

- Reddit data collection through Reddit API
- Sentiment analysis using advanced NLP models
- Data processing with Apache Hadoop and Spark
- Modern JavaFX-based user interface
- Support for both keyword and subreddit-based analysis

## Prerequisites

- Java JDK 11 or higher
- Python 3.8 or higher
- Maven
- Apache Hadoop 3.x
- Apache Spark 3.x

## Setup Instructions

### 1. Basic Setup

1. Clone the repository:
   ```bash
   git clone [your-repository-url]
   cd social-media-sentiment-analysis
   ```

2. Configure the environment:
   - Copy `src/main/resources/config.properties.template` to `src/main/resources/config.properties`
   - Update the config file with your API credentials:
     - Reddit API credentials (get from https://www.reddit.com/prefs/apps)
     - Hugging Face API key (if using their models)

3. Install Python dependencies:
   ```bash
   python -m venv venv
   venv\Scripts\activate  # On Windows
   pip install -r requirements.txt
   ```

### 2. Hadoop/Spark Setup

#### Option A: Local Setup
1. Install Hadoop and Spark locally
2. Set HADOOP_HOME and SPARK_HOME environment variables
3. Update paths in `config.properties`

#### Option B: Docker Setup
1. Install Docker and Docker Compose
2. Use the provided docker-compose file:
   ```bash
   docker-compose up -d
   ```
3. Update the Hadoop/Spark connection settings in `config.properties` to point to Docker containers

#### Option C: WSL Setup
1. Install Hadoop and Spark in WSL
2. Configure WSL network settings
3. Update `config.properties` with WSL endpoints
4. Ensure proper permissions between Windows and WSL

## Running the Application

### Using run.bat (Recommended for Windows)
Simply execute the `run.bat` file:
```bash
run.bat
```

### Manual Execution
```bash
mvn clean install
mvn javafx:run
```

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/sentimentanalysis/
│   │       ├── application/    # Main application
│   │       ├── config/        # Configuration
│   │       ├── core/          # Core logic
│   │       ├── ui/           # JavaFX UI
│   │       └── util/         # Utilities
│   ├── python/               # Python scripts
│   └── resources/            # Configuration files
```

## Configuration

The application requires several configuration parameters in `config.properties`:

1. Reddit API credentials
2. Hadoop/Spark connection details
3. Python environment settings

## Troubleshooting

### Common Issues

1. Hadoop Connection Issues:
   - Verify Hadoop services are running
   - Check network connectivity
   - Ensure proper permissions

2. Python Integration:
   - Verify Python path in config
   - Check virtual environment activation
   - Validate package installations

3. Reddit API:
   - Verify API credentials
   - Check rate limits
   - Ensure proper authentication

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

[Your License Here]
