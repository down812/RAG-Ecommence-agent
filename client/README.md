# RAGGuideAgent Android

## API configuration

Set the remote backend URL in the ignored `local.properties` file:

```properties
API_BASE_URL=https://your-service.example.com/
```

The URL must include `http://` or `https://`. HTTPS is recommended. Do not put model
API keys in the Android project; the app only calls the project backend.
