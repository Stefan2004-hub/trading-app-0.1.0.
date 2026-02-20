import { useEffect, useMemo, useState } from 'react';

export interface LookupOption {
  id: string;
  name: string;
  symbol?: string;
}

interface SearchableLookupFieldProps {
  id: string;
  label: string;
  placeholder: string;
  required?: boolean;
  value: LookupOption | null;
  onSelect: (option: LookupOption | null) => void;
  onSearch: (search: string) => Promise<LookupOption[]>;
  quickAddLabel?: string;
  onQuickAdd?: (search: string) => Promise<LookupOption>;
}

function formatOption(option: LookupOption): string {
  if (option.symbol) {
    return `${option.symbol} - ${option.name}`;
  }
  return option.name;
}

export function SearchableLookupField({
  id,
  label,
  placeholder,
  required = false,
  value,
  onSelect,
  onSearch,
  quickAddLabel,
  onQuickAdd
}: SearchableLookupFieldProps): JSX.Element {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<LookupOption[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    setQuery(value ? formatOption(value) : '');
  }, [value]);

  useEffect(() => {
    if (!open) {
      return;
    }
    const timeoutId = window.setTimeout(() => {
      void (async () => {
        setLoading(true);
        try {
          setResults(await onSearch(query));
        } finally {
          setLoading(false);
        }
      })();
    }, 300);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [open, onSearch, query]);

  const canQuickAdd = useMemo(
    () => Boolean(onQuickAdd && query.trim() && !loading && results.length === 0),
    [loading, onQuickAdd, query, results.length]
  );

  return (
    <div className="autocomplete-field">
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        type="search"
        value={query}
        placeholder={placeholder}
        onFocus={() => setOpen(true)}
        onBlur={() => {
          window.setTimeout(() => setOpen(false), 120);
        }}
        onChange={(event) => {
          setQuery(event.target.value);
          onSelect(null);
          setOpen(true);
        }}
      />
      <input type="hidden" value={value?.id ?? ''} required={required} />
      {open ? (
        <div className="autocomplete-dropdown">
          {loading ? <div className="autocomplete-empty">Searching...</div> : null}
          {!loading && results.length === 0 ? <div className="autocomplete-empty">No results</div> : null}
          {!loading
            ? results.map((option) => (
                <button
                  key={option.id}
                  type="button"
                  className="autocomplete-option"
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => {
                    onSelect(option);
                    setQuery(formatOption(option));
                    setOpen(false);
                  }}
                >
                  {formatOption(option)}
                </button>
              ))
            : null}
          {canQuickAdd ? (
            <button
              type="button"
              className="autocomplete-quick-add"
              onMouseDown={(event) => event.preventDefault()}
              onClick={async () => {
                if (!onQuickAdd) {
                  return;
                }
                const created = await onQuickAdd(query.trim());
                onSelect(created);
                setQuery(formatOption(created));
                setOpen(false);
              }}
            >
              {quickAddLabel}
            </button>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
