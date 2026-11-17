
import React, { useEffect, useRef, useState } from 'react';
import './CustomSelect.css';

export default function CustomSelect({
    id,
    name,
    value,
    onChange,
    options = [],
    placeholder = 'Sélectionner...',
    disabled = false,
    required = false,
    className = '',
}) {
    const [isOpen, setIsOpen] = useState(false);
    const [activeIndex, setActiveIndex] = useState(-1);
    const containerRef = useRef(null);

    const normalizedOptions = options.map((option) => ({
        value: String(option.value ?? ''),
        label: String(option.label ?? ''),
    }));

    const selectedOption = normalizedOptions.find(
        (option) => option.value === String(value ?? '')
    );

    useEffect(() => {
        function handleOutsideClick(event) {
            if (
                containerRef.current &&
                !containerRef.current.contains(event.target)
            ) {
                setIsOpen(false);
                setActiveIndex(-1);
            }
        }

        document.addEventListener('mousedown', handleOutsideClick);

        return () => {
            document.removeEventListener('mousedown', handleOutsideClick);
        };
    }, []);

    useEffect(() => {
        if (!isOpen) {
            setActiveIndex(-1);
        }
    }, [isOpen]);

    const selectOption = (option) => {
        if (disabled) return;

        // Format conservé pour rester compatible avec les handlers existants.
        onChange?.({
            target: {
                id,
                name,
                value: option.value,
            },
        });

        setIsOpen(false);
        setActiveIndex(-1);
    };

    const handleKeyDown = (event) => {
        if (disabled) return;

        if (event.key === 'Escape') {
            setIsOpen(false);
            setActiveIndex(-1);
            return;
        }

        if (event.key === 'ArrowDown') {
            event.preventDefault();

            if (!isOpen) {
                setIsOpen(true);
                setActiveIndex(0);
                return;
            }

            setActiveIndex((current) =>
                Math.min(current + 1, normalizedOptions.length - 1)
            );
        }

        if (event.key === 'ArrowUp') {
            event.preventDefault();

            if (!isOpen) {
                setIsOpen(true);
                setActiveIndex(normalizedOptions.length - 1);
                return;
            }

            setActiveIndex((current) => Math.max(current - 1, 0));
        }

        if (event.key === 'Enter' && isOpen && activeIndex >= 0) {
            event.preventDefault();
            selectOption(normalizedOptions[activeIndex]);
        }

        if (event.key === 'Enter' && !isOpen) {
            event.preventDefault();
            setIsOpen(true);
        }
    };

    return (
        <div
            ref={containerRef}
            className={`custom-select ${className} ${
                isOpen ? 'is-open' : ''
            } ${disabled ? 'is-disabled' : ''}`}
        >
            <button
                id={id}
                type="button"
                className="custom-select__trigger"
                onClick={() => !disabled && setIsOpen((open) => !open)}
                onKeyDown={handleKeyDown}
                disabled={disabled}
                aria-haspopup="listbox"
                aria-expanded={isOpen}
                aria-controls={`${id}-listbox`}
                aria-required={required}
            >
                <span className={!selectedOption ? 'is-placeholder' : ''}>
                    {selectedOption?.label || placeholder}
                </span>

                <svg
                    className="custom-select__arrow"
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                >
                    <path
                        d="m7 10 5 5 5-5"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                    />
                </svg>
            </button>

            {required && (
                <input
                    className="custom-select__validation"
                    type="text"
                    tabIndex={-1}
                    aria-hidden="true"
                    value={value ?? ''}
                    required
                    readOnly
                    onChange={() => {}}
                    onInvalid={() => {
                        setIsOpen(true);
                    }}
                />
            )}

            {isOpen && (
                <div
                    id={`${id}-listbox`}
                    className="custom-select__dropdown"
                    role="listbox"
                    aria-labelledby={id}
                >
                    {normalizedOptions.length > 0 ? (
                        normalizedOptions.map((option, index) => (
                            <button
                                key={`${option.value}-${index}`}
                                type="button"
                                role="option"
                                aria-selected={
                                    String(value ?? '') === option.value
                                }
                                className={`custom-select__option ${
                                    String(value ?? '') === option.value
                                        ? 'is-selected'
                                        : ''
                                } ${
                                    activeIndex === index
                                        ? 'is-active'
                                        : ''
                                }`}
                                onMouseEnter={() => setActiveIndex(index)}
                                onClick={() => selectOption(option)}
                            >
                                <span>{option.label}</span>

                                {String(value ?? '') === option.value && (
                                    <span className="custom-select__check">
                                        ✓
                                    </span>
                                )}
                            </button>
                        ))
                    ) : (
                        <div className="custom-select__empty">
                            Aucune option disponible
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
