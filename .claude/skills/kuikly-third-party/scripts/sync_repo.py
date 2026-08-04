#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Git Repository Sync Script for Kuikly Third-Party Components

This script manages the KuiklyUI-third-party repository in the references directory:
- Clones the repository if it doesn't exist
- Checks if the repository needs updating (last update > 1 week)
- Performs git pull if needed

Usage:
    python sync_repo.py [--force]

Options:
    --force     Force git pull regardless of last update time
"""

import sys
import subprocess
from pathlib import Path
from datetime import datetime, timedelta


def get_repo_path():
    """Get the path to the KuiklyUI-third-party repository"""
    script_dir = Path(__file__).parent
    skill_dir = script_dir.parent
    repo_path = skill_dir / 'references' / 'KuiklyUI-third-party'
    return repo_path


def run_command(cmd, cwd=None):
    """Run a shell command and return output"""
    try:
        result = subprocess.run(
            cmd,
            cwd=cwd,
            capture_output=True,
            text=True,
            check=True
        )
        return result.stdout.strip(), None
    except subprocess.CalledProcessError as e:
        return None, e.stderr.strip()


def repo_exists(repo_path):
    """Check if repository exists and is a valid git repo"""
    if not repo_path.exists():
        return False
    
    git_dir = repo_path / '.git'
    return git_dir.exists() and git_dir.is_dir()


def clone_repository(repo_path):
    """Clone the KuiklyUI-third-party repository"""
    repo_url = 'https://github.com/Tencent-TDS/KuiklyUI-third-party.git'
    parent_dir = repo_path.parent
    
    print(f"Cloning repository from {repo_url}...")
    parent_dir.mkdir(parents=True, exist_ok=True)
    
    _, stderr = run_command(
        ['git', 'clone', repo_url, str(repo_path.name)],
        cwd=parent_dir
    )
    
    if stderr:
        print(f"Error cloning repository: {stderr}")
        return False
    
    print("Repository cloned successfully!")
    return True


def get_last_pull_time(repo_path):
    """Get the timestamp of the last git pull (or clone)"""
    # Check FETCH_HEAD file modification time
    fetch_head = repo_path / '.git' / 'FETCH_HEAD'
    
    if not fetch_head.exists():
        # If FETCH_HEAD doesn't exist, use .git directory modification time
        git_dir = repo_path / '.git'
        if git_dir.exists():
            return datetime.fromtimestamp(git_dir.stat().st_mtime)
        return None
    
    return datetime.fromtimestamp(fetch_head.stat().st_mtime)


def needs_update(repo_path):
    """Check if repository needs update (> 1 week since last pull)"""
    last_pull = get_last_pull_time(repo_path)
    
    if last_pull is None:
        return True
    
    time_since_pull = datetime.now() - last_pull
    one_week = timedelta(weeks=1)
    
    if time_since_pull > one_week:
        days = time_since_pull.days
        print(f"Repository last updated {days} days ago (> 7 days), update needed")
        return True
    
    days = time_since_pull.days
    print(f"Repository last updated {days} days ago, no update needed")
    return False


def pull_repository(repo_path):
    """Pull latest changes from repository"""
    print("Pulling latest changes...")
    
    stdout, stderr = run_command(['git', 'pull'], cwd=repo_path)
    
    if stderr and 'error' in stderr.lower():
        print(f"Error pulling repository: {stderr}")
        return False
    
    if stdout:
        print(stdout)
    
    print("Repository updated successfully!")
    return True


def main():
    """Main function"""
    force_update = '--force' in sys.argv
    
    repo_path = get_repo_path()
    
    # Check if repository exists
    if not repo_exists(repo_path):
        print("Repository not found, cloning...")
        if not clone_repository(repo_path):
            sys.exit(1)
        print(f"Repository location: {repo_path}")
        sys.exit(0)
    
    print(f"Repository found at: {repo_path}")
    
    # Check if update is needed
    if force_update:
        print("Force update requested")
        if not pull_repository(repo_path):
            sys.exit(1)
    elif needs_update(repo_path):
        if not pull_repository(repo_path):
            sys.exit(1)
    
    sys.exit(0)


if __name__ == "__main__":
    main()
