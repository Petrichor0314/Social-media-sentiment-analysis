import praw
import csv
import sys
import logging
import configparser
from pathlib import Path

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

def load_config():
    try:
        config = configparser.ConfigParser()
        config_path = Path(__file__).parent.parent / 'resources' / 'config.properties'
        logging.info(f"Loading config from: {config_path}")
        
        if not config_path.exists():
            raise FileNotFoundError(f"Config file not found at {config_path}")
            
        config.read(config_path)
        
        return {
            'client_id': config.get('RedditAPI', 'client_id'),
            'client_secret': config.get('RedditAPI', 'client_secret'),
            'user_agent': config.get('RedditAPI', 'user_agent')
        }
    except Exception as e:
        logging.error(f"Error loading config: {str(e)}")
        raise

def fetch_comments_from_post(post_url, comments_per_post, output_file):
    """
    Fetch comments from a specific Reddit post and save to CSV.

    :param post_url: The URL of the Reddit post.
    :param comments_per_post: The number of comments to fetch.
    :param output_file: Path to save the CSV file.
    """
    try:
        # Initialize PRAW with Reddit API credentials from config
        config = load_config()
        reddit = praw.Reddit(
            client_id=config['client_id'],
            client_secret=config['client_secret'],
            user_agent=config['user_agent']
        )
        logging.info(f"Fetching comments from post: {post_url}")
        
        submission = reddit.submission(url=post_url)
        submission.comments.replace_more(limit=0)
        comments = submission.comments[:int(comments_per_post)]

        with open(output_file, mode="w", newline="", encoding="utf-8") as csvfile:
            writer = csv.writer(csvfile)
            writer.writerow(["Post Title", "Comment"])
            for comment in comments:
                writer.writerow([submission.title, comment.body])
                
        logging.info(f"Successfully saved comments to {output_file}")
        
    except Exception as e:
        logging.error(f"Error in fetch_comments_from_post: {str(e)}")
        raise

if __name__ == "__main__":
    try:
        logging.info(f"Script started with arguments: {sys.argv}")
        if len(sys.argv) != 4:
            raise ValueError("Incorrect number of arguments")
            
        post_url = sys.argv[1]
        comments_per_post = sys.argv[2]
        output_file = sys.argv[3]
        
        fetch_comments_from_post(post_url, comments_per_post, output_file)
        logging.info("Script completed successfully")
    except Exception as e:
        logging.error(f"Script failed: {str(e)}")
        sys.exit(1)
