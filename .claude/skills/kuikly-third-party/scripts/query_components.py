#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Kuikly Third-Party Components Query Script

This script reads the KuiklyUI-Libraries.json file and queries available components.
It provides functionality to:
- List all available components
- Search components by name or description
- Get details about a specific component including GitHub URL

Usage:
    python query_components.py list
    python query_components.py search <keyword>
    python query_components.py get <component-name>
"""

import json
import sys
from pathlib import Path


def get_libraries_json_path():
    """Get the path to KuiklyUI-Libraries.json"""
    script_dir = Path(__file__).parent
    skill_dir = script_dir.parent
    json_path = skill_dir / 'references' / 'KuiklyUI-third-party' / 'KuiklyUI-Libraries.json'
    return json_path


def load_libraries():
    """Load the libraries data from JSON file"""
    json_path = get_libraries_json_path()
    
    if not json_path.exists():
        print(f"Error: KuiklyUI-Libraries.json not found at {json_path}")
        print("Please run sync_repo.py first to clone the repository")
        return None
    
    try:
        with open(json_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        return data
    except json.JSONDecodeError as e:
        print(f"Error parsing JSON file: {e}")
        return None
    except OSError as e:
        print(f"Error reading file: {e}")
        return None


def list_components(data):
    """List all available components"""
    if not data:
        return
    
    # Data is directly an array of libraries
    libraries = data if isinstance(data, list) else []
    
    if not libraries:
        print("No libraries found in the JSON file")
        return
    
    print(f"\nTotal components: {len(libraries)}\n")
    print("-" * 80)
    
    for idx, lib in enumerate(libraries, 1):
        name = lib.get('componentName', 'Unknown')
        description = lib.get('componentDescription', 'No description')
        github_url = lib.get('githubUrl', 'N/A')
        comp_type = lib.get('componentType', 'N/A')
        
        print(f"{idx}. {name}")
        print(f"   Type: {comp_type}")
        print(f"   Description: {description}")
        print(f"   GitHub: {github_url}")
        print("-" * 80)


def search_components(data, keyword):
    """Search components by keyword in name or description"""
    if not data:
        return
    
    # Data is directly an array of libraries
    libraries = data if isinstance(data, list) else []
    keyword_lower = keyword.lower()
    
    matches = []
    for lib in libraries:
        name = lib.get('componentName', '').lower()
        description = lib.get('componentDescription', '').lower()
        
        if keyword_lower in name or keyword_lower in description:
            matches.append(lib)
    
    if not matches:
        print(f"No components found matching '{keyword}'")
        return
    
    print(f"\nFound {len(matches)} component(s) matching '{keyword}':\n")
    print("-" * 80)
    
    for idx, lib in enumerate(matches, 1):
        name = lib.get('componentName', 'Unknown')
        description = lib.get('componentDescription', 'No description')
        github_url = lib.get('githubUrl', 'N/A')
        comp_type = lib.get('componentType', 'N/A')
        
        print(f"{idx}. {name}")
        print(f"   Type: {comp_type}")
        print(f"   Description: {description}")
        print(f"   GitHub: {github_url}")
        print("-" * 80)


def get_component_details(data, component_name):
    """Get detailed information about a specific component"""
    if not data:
        return None
    
    # Data is directly an array of libraries
    libraries = data if isinstance(data, list) else []
    
    # Try exact match first
    for lib in libraries:
        if lib.get('componentName', '').lower() == component_name.lower():
            return lib
    
    # Try partial match
    for lib in libraries:
        if component_name.lower() in lib.get('componentName', '').lower():
            return lib
    
    return None


def display_component_details(component):
    """Display detailed information about a component"""
    if not component:
        return
    
    print("\n" + "=" * 80)
    print(f"Component: {component.get('componentName', 'Unknown')}")
    print("=" * 80)
    print(f"\nType: {component.get('componentType', 'N/A')}")
    print(f"Developer: {component.get('developer', 'N/A')}")
    print(f"\nDescription: {component.get('componentDescription', 'No description')}")
    print(f"\nGitHub URL: {component.get('githubUrl', 'N/A')}")
    
    # Display platform support
    platforms = []
    for platform in ['Android', 'iOS', 'Ohos', 'Js', 'Macos', 'Linux', 'Windows', 'Tvos']:
        if component.get(platform, False):
            platforms.append(platform)
    
    if platforms:
        print(f"\nSupported Platforms: {', '.join(platforms)}")
    
    # Display examples if present
    examples = component.get('examples', [])
    if examples:
        print(f"\nExamples: {len(examples)} available")
    
    print("=" * 80)


def print_usage():
    """Print usage instructions"""
    print("""
Usage:
    python query_components.py list                    - List all components
    python query_components.py search <keyword>        - Search components
    python query_components.py get <component-name>    - Get component details

Examples:
    python query_components.py list
    python query_components.py search chart
    python query_components.py get echarts
""")


def main():
    """Main function"""
    if len(sys.argv) < 2:
        print_usage()
        sys.exit(1)
    
    command = sys.argv[1].lower()
    
    # Load libraries data
    data = load_libraries()
    if data is None:
        sys.exit(1)
    
    if command == 'list':
        list_components(data)
    
    elif command == 'search':
        if len(sys.argv) < 3:
            print("Error: Please provide a search keyword")
            print_usage()
            sys.exit(1)
        keyword = sys.argv[2]
        search_components(data, keyword)
    
    elif command == 'get':
        if len(sys.argv) < 3:
            print("Error: Please provide a component name")
            print_usage()
            sys.exit(1)
        component_name = sys.argv[2]
        component = get_component_details(data, component_name)
        if component:
            display_component_details(component)
            # Return GitHub URL as the last line for easy parsing
            print(f"\nGitHub_URL: {component.get('githubUrl', '')}")
        else:
            print(f"Component '{component_name}' not found")
            sys.exit(1)
    
    else:
        print(f"Unknown command: {command}")
        print_usage()
        sys.exit(1)


if __name__ == "__main__":
    main()
