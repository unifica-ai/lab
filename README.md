# Unifica lab

This project contains integration code and customer notebooks.

Note these directories:

`a` - Customer-specific source, assets and notebooks
`libs` - Supporting libraries

## Building

See README files in `lib/amzn-sp`, `lib/amzn-sp-reports`

In customer dicrectory, generate .class files for dependencies:

```
cd a/luum
clj -X:deps prep
```

## Testing

```
make test
```
