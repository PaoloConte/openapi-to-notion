Experimental utility to convert OpenAPI definitions to Notion Pages via the Notion API.

# Usage
- Create an internal Notion integration and get the secret token
- The Target Notion page needs to be shared with the integration
- The OpenAPI definitions need to have a unique title which will be used as the page title
- Run the program locally or as a GitHub action

# Options
OpenAPI extensions:
- `x-notion-flatten` : Flatten the object structure instead of linking to schemas (default false).

# GitHub Action
```yaml
jobs:
  openapi-to-notion:
    runs-on: ubuntu-latest
    env:
      APP_VERSION: 'v1.0.0'
    steps:
      - name: Checkout code
        uses: actions/checkout@v3
      - name: Build API docs
        uses: PaoloConte/openapi-to-notion@main
        with:
          openapi-folder: 'docs/folder'
          target-page: '111112222233334444455556667777' 
          app-version: ${{ env.APP_VERSION }}
          notion-token: ${{ secrets.NOTION_TOKEN }}
```
# Run Locally

### Requirements
- Java 11+

### Build
```bash
./gradlew shadowJar
```

### Environment Variables
- `NOTION_TOKEN` : The Notion integration API token
- `TARGET_PAGE` : ID of the Notion page where to create the new pages into

### Program Arguments
- path of the folder containing the OpenAPI definitions

### Run
```bash
export TARGET_PAGE=1234567890
export NOTION_TOKEN=secret_ababababababababababba
java -jar build/libs/app.jar /path/to/folder
```

# Test Screenshot
![screenshot](screenshot.png)