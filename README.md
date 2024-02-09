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
          notion-token: ${{ secrets.NOTION_TOKEN }}
          config: |
            pages:
              - notionPageId: aaaaaaaaaaaaaaa
                apiFolder: path/to/docs
              - notionPageId: bbbbbbbbbbbbbbb
                apiFolder: path/to/other  

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

### Program Arguments
- path of the folder containing the OpenAPI definitions

### Run
Create a `config.yaml` file with the following content:
```yaml
pages:
  - notionPageId: aaaaaaaaaaaaaaa
    apiFolder: path/to/docs
  - notionPageId: bbbbbbbbbbbbbbb
    apiFolder: path/to/other
```
Execute
```bash
export NOTION_TOKEN=secret_ababababababababababba
java -jar build/libs/app.jar --config-file config.yaml 
```

# Test Screenshot
![screenshot](screenshot.png)