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

def search_comments_by_keyword(keyword, num_posts, comments_per_post, sort_by, output_file):
    """
    Search Reddit for comments containing a keyword and save to CSV.

    :param keyword: The keyword to search for.
    :param num_posts: The number of posts to fetch.
    :param comments_per_post: The number of comments to fetch per post.
    :param sort_by: Sorting option ('relevance', 'hot', 'top', 'new', 'comments').
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
        logging.info(f"Searching for keyword: {keyword}")
        
        search_results = reddit.subreddit("all").search(query=keyword, sort=sort_by, limit=int(num_posts))

        with open(output_file, mode="w", newline="", encoding="utf-8") as csvfile:
            writer = csv.writer(csvfile)
            writer.writerow(["Subreddit", "Post Title", "Comment"])

            for submission in search_results:
                try:
                    submission.comments.replace_more(limit=0)
                    comments = submission.comments[:int(comments_per_post)]
                    for comment in comments:
                        writer.writerow([submission.subreddit.display_name, submission.title, comment.body])
                except Exception as e:
                    logging.warning(f"Error processing submission: {str(e)}")
                    continue
                    
        logging.info(f"Successfully saved results to {output_file}")
        
    except Exception as e:
        logging.error(f"Error in search_comments_by_keyword: {str(e)}")
        raise

if __name__ == "__main__":
    try:
        logging.info(f"Script started with arguments: {sys.argv}")
        if len(sys.argv) != 6:
            raise ValueError("Incorrect number of arguments")
            
        keyword = sys.argv[1]
        num_posts = sys.argv[2]
        comments_per_post = sys.argv[3]
        sort_by = sys.argv[4]
        output_file = sys.argv[5]
        
        search_comments_by_keyword(keyword, num_posts, comments_per_post, sort_by, output_file)
        logging.info("Script completed successfully")
    except Exception as e:
        logging.error(f"Script failed: {str(e)}")
        sys.exit(1)
