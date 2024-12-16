# Social Media Sentiment Analysis Application

A Java/Python application for analyzing sentiment from social media content, built with JavaFX for the GUI and Python for data processing.

## Prerequisites

- Java JDK 11 or higher
- Python 3.8 or higher
- Maven

## Setup Instructions

1. Clone the repository
2. Install Java dependencies:
   ```
   mvn install
   ```
3. Set up Python virtual environment:
   ```
   python -m venv venv
   venv\Scripts\activate  # On Windows
   pip install -r requirements.txt
   ```
4. Run the application:
   ```
   mvn javafx:run
   ```

## Project Structure

- `src_new/main/java`: Java source files for the GUI and application logic
- `src_new/main/python`: Python scripts for data processing
- `data`: Data storage directory

## Features

- Reddit data fetching and analysis
- Sentiment analysis of social media content
- Interactive GUI for data visualization
