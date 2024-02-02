Experimental utility to convert OpenAPI definitions to Notion Pages via the Notion API.

**Note**: Only really works with OpenAPI 3.0 definitions. Version 3.1 will need some adjustments.

### Requirements
- Java 11+

### Build
```bash
./gradlew build shadowJar
```

### Environment Variables
- `NOTION_TOKEN` : The Notion integration API token
- `TARGET_PAGE` : ID of the Notion page where to create the new pages into

### Program Arguments
- path of the folder containing the OpenAPI definitions

### Usage
- Create an internal Notion integration and get the secret token
- The Target Notion page needs to be shared with the integration
- The OpenAPI definitions need to have a unique title which will be used as the page title
- Run the program with the environment variables and the path to the folder containing the OpenAPI definitions

```bash
export TARGET_PAGE=1234567890
export NOTION_TOKEN=secret_ababababababababababba
java -jar build/libs/app.jar /path/to/folder
```

### Options
OpenAPI extensions:
- `x-notion-flatten` : Flatten the object structure instead of linking to schemas (default false).

# Test Screenshot
![screenshot](screenshot.png)