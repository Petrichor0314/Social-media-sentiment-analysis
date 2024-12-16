import praw
import csv
import sys
import logging
import configparser
import traceback
from pathlib import Path

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

def load_config():
    try:
        config = configparser.ConfigParser()
        config_path = Path(__file__).parent.parent / 'resources' / 'config.properties'
        logging.info(f"Loading config from: {config_path}")
        
        if not config_path.exists():
            alt_config_path = Path(__file__).parent.parent.parent / 'resources' / 'config.properties'
            logging.info(f"Config not found, trying alternative path: {alt_config_path}")
            if alt_config_path.exists():
                config_path = alt_config_path
            else:
                raise FileNotFoundError(f"Config file not found at {config_path} or {alt_config_path}")
            
        config.read(config_path)
        
        return {
            'client_id': config.get('RedditAPI', 'client_id'),
            'client_secret': config.get('RedditAPI', 'client_secret'),
            'user_agent': config.get('RedditAPI', 'user_agent')
        }
    except Exception as e:
        logging.error(f"Error loading config: {str(e)}")
        raise

def fetch_comments_from_subreddit(subreddit_name, num_posts, comments_per_post, sort_by, output_file):
    """
    Fetch comments from a subreddit and save to CSV.

    :param subreddit_name: The subreddit to fetch comments from.
    :param num_posts: The number of posts to fetch.
    :param comments_per_post: The number of comments to fetch per post.
    :param sort_by: Sorting option ('hot', 'new', 'top', 'rising').
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
        logging.info(f"Fetching from subreddit: {subreddit_name}")
        
        try:
            subreddit = reddit.subreddit(subreddit_name)
            sort_func = getattr(subreddit, sort_by)
            posts = list(sort_func(limit=int(num_posts)))
            logging.info(f"Successfully fetched {len(posts)} posts")
        except Exception as e:
            logging.error(f"Error accessing subreddit: {str(e)}")
            raise ValueError(f"Could not access subreddit '{subreddit_name}'. Make sure it exists and is public.")

        with open(output_file, mode="w", newline="", encoding="utf-8") as csvfile:
            writer = csv.writer(csvfile)
            writer.writerow(["Subreddit", "Post Title", "Comment"])

            for submission in posts:
                try:
                    submission.comments.replace_more(limit=0)
                    comments = submission.comments[:int(comments_per_post)]
                    for comment in comments:
                        if hasattr(comment, 'body'):  # Make sure it's a valid comment
                            writer.writerow([subreddit_name, submission.title, comment.body])
                except Exception as e:
                    logging.warning(f"Error processing submission {submission.id}: {str(e)}")
                    continue
                    
        logging.info(f"Successfully saved comments to {output_file}")
        
    except Exception as e:
        logging.error(f"Error in fetch_comments_from_subreddit: {str(e)}")
        logging.error(f"Traceback: {traceback.format_exc()}")
        raise

if __name__ == "__main__":
    try:
        logging.info(f"Script started with arguments: {sys.argv[1:]}")
        if len(sys.argv) != 6:
            raise ValueError(f"Incorrect number of arguments. Expected 6, got {len(sys.argv)}")
            
        subreddit_name = sys.argv[1].strip()
        try:
            num_posts = int(sys.argv[2])
            comments_per_post = int(sys.argv[3])
        except ValueError as e:
            logging.error(f"Failed to convert numeric arguments: {e}")
            logging.error(f"num_posts = {sys.argv[2]}, comments_per_post = {sys.argv[3]}")
            raise ValueError(f"Invalid numeric arguments: {e}")
            
        sort_by = sys.argv[4]
        output_file = sys.argv[5]
        
        logging.info(f"Fetching from subreddit: {subreddit_name}")
        logging.info(f"Number of posts: {num_posts}")
        logging.info(f"Comments per post: {comments_per_post}")
        logging.info(f"Sort by: {sort_by}")
        logging.info(f"Output file: {output_file}")
        
        fetch_comments_from_subreddit(subreddit_name, num_posts, comments_per_post, sort_by, output_file)
        logging.info("Script completed successfully")
    except Exception as e:
        logging.error(f"Script failed: {str(e)}")
        logging.error(f"Traceback: {traceback.format_exc()}")
        sys.exit(1)
