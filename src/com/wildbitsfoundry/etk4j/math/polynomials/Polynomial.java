package com.wildbitsfoundry.etk4j.math.polynomials;

import java.util.Arrays;

import com.wildbitsfoundry.etk4j.math.complex.Complex;
import com.wildbitsfoundry.etk4j.math.functions.UnivariateFunction;
import com.wildbitsfoundry.etk4j.math.linearalgebra.EigenvalueDecomposition;
import com.wildbitsfoundry.etk4j.math.linearalgebra.Matrices;
import com.wildbitsfoundry.etk4j.math.linearalgebra.Matrix;
import com.wildbitsfoundry.etk4j.util.ArrayUtils;

/**
 * 
 * @author Marcos L. Lopez-Rivera
 * @version
 *
 */
public class Polynomial implements UnivariateFunction {

	protected double[] _coefs = null;
	protected Complex[] _roots = null;

	/***
	 * Default constructor. Creates a polynomial with a constant value of 1.
	 */
	public Polynomial() {
		_coefs = new double[] { 1.0 };
	}

	/***
	 * Copy constructor
	 */
	public Polynomial(Polynomial polynomial) {
		int length = polynomial._coefs.length;
		this._coefs = new double[length];
		System.arraycopy(polynomial._coefs, 0, this._coefs, 0, length);
		if (polynomial._roots != null) {
			_roots = new Complex[polynomial._roots.length];
			System.arraycopy(polynomial._roots, 0, _roots, 0, polynomial._roots.length);
		}

	}

	/***
	 * Creates a polynomial P(x) and initializes its coefficients to the
	 * coefficients passed as parameters. The coefficients are assumed to be in
	 * descending order i.e. [1, 3, 2] will generate P(x) = x^2 + 3x + 2
	 * 
	 * @param coefficients
	 *            Array of coefficients in descending order
	 */
	public Polynomial(double... coefficients) {

		int length = coefficients.length;
		int i = 0;
		while (i < length && coefficients[i] == 0.0) {
			++i;
		}
		this._coefs = i == length ? new double[] { 1.0 } : Arrays.copyOfRange(coefficients, i, length);

	}

	/***
	 * Creates a polynomial P(x) from an array of its roots. Say we have the
	 * following set of roots r = [ -1, -2 ] then: P(x) = (x + 1)*(x + 2) which
	 * is the solution to the polynomial: P(x) = x^2 + 3x + 2
	 * 
	 * @param roots
	 *            Array of roots
	 */
	public Polynomial(Complex... roots) {
		final int size = roots.length;

		Complex[] tmp = new Complex[size];
		Complex[] result = new Complex[size + 1];

		for(int i = 1; i <= size; ++i) {
			result[i] = new Complex();
		}
		result[0] = new Complex(1.0, 0.0);

		for (int i = 0; i < size; ++i) {
			// Fill up tmp
			for (int j = 0; j <= i; ++j) {
				tmp[j] = roots[i].multiply(result[j]);
			}
			for (int j = 0; j <= i; ++j) {
				result[j + 1].subtractEquals(tmp[j]);
			}
		}
		_coefs = new double[size + 1];
		for (int i = 0; i <= size; i++) {
			_coefs[i] = result[i].real();
		}
		_roots = Arrays.copyOf(roots, size);
	}

	/***
	 * Reverts the normalize process. See normalize function.
	 */
	public void denormalize() {
		int i;
		// Find the normalizing gain
		i = _coefs.length - 1;
		while (this._coefs[i] < Double.MIN_VALUE) {
			--i;
			if (i == 0)
				break;
		}
		for (int j = 0; j < this._coefs.length; j++) {
			this._coefs[j] = this._coefs[j] / this._coefs[i];
		}
	}

	/***
	 * Forces the highest order coefficient of the polynomial to be unity by
	 * dividing all the other coefficients by the highest order coefficient.
	 * 
	 */
	public double normalize() {
		double cn = this._coefs[0];
		for (int j = 1; j < this._coefs.length; j++) {
			this._coefs[j] = this._coefs[j] / cn;
		}
		this._coefs[0] = 1;
		return cn;
	}

	/***
	 * Multiply two polynomials
	 * 
	 * @param p
	 *            Another polynomial
	 * @return Pnew(x) = P(x) * poly
	 */
	public Polynomial multiply(final Polynomial p) {
		return new Polynomial(ArrayUtils.conv(_coefs, p._coefs));
	}

	/***
	 * Multiply two polynomials and stores the result
	 * 
	 * @param p
	 *            Another polynomial
	 */
	public void multiplyEquals(final Polynomial p) {
		_coefs = ArrayUtils.conv(_coefs, p._coefs);
		_roots = null;
	}

	public Polynomial multiply(double d) {
		return new Polynomial(ArrayUtils.conv(_coefs, d));
	}

	public void multiplyEquals(double d) {
		for (int i = 0; i < this._coefs.length; i++) {
			this._coefs[i] = this._coefs[i] * d;
		}
	}

	public Polynomial add(final Polynomial p) {
		double[] result = new double[Math.max(_coefs.length, p._coefs.length)];
		addOp(this, p, result);
		return new Polynomial(result);
	}

	public void addEquals(final Polynomial p) {
		addOp(this, p, _coefs);
		_roots = null;
	}

	private static final void addOp(Polynomial p1, Polynomial p2, double[] result) {
		final int p1Length = p1._coefs.length - 1;
		final int p2Length = p2._coefs.length - 1;
		final int rLength = result.length - 1;
		for(int i = p1Length, j = p2Length, k = rLength; i >= 0 && j >= 0; --i, --j, --k) {
			result[k] = p1._coefs[i] + p2._coefs[j];
		}
	}


	public Polynomial subtract(final Polynomial p) {
		double[] result = new double[Math.max(_coefs.length, p._coefs.length)];
		subtractOp(this, p, result);
		return new Polynomial(result);
	}
	
	public void subtractEquals(final Polynomial p) {
		subtractOp(this, p, _coefs);
		_roots = null;
	}
	
	private static final void subtractOp(Polynomial p1, Polynomial p2, double[] result) {
		final int p1Length = p1._coefs.length - 1;
		final int p2Length = p2._coefs.length - 1;
		final int rLength = result.length - 1;
		for(int i = p1Length, j = p2Length, k = rLength; i >= 0 && j >= 0; --i, --j, --k) {
			result[k] = p1._coefs[i] - p2._coefs[j];
		}
	}


	/***
	 * 
	 * @return Returns the order of the polynomial
	 */
	public int degree() {
		return _coefs.length - 1;
	}

	@Override
	public String toString() {
		java.lang.StringBuilder sb = new java.lang.StringBuilder();
		int order = this.degree();
		sb.append(String.format("%.4f", this._coefs[0]));
		sb.append(" * x^").append(order);
		for (int i = order - 1; i >= 0; i--) {
			if (this._coefs[order - i] != 0.0) {
				sb.append(i < order ? Math.signum(this._coefs[order - i]) < 0 ? " - " : " + " : "")
						.append(String.format("%.4f", Math.abs(this._coefs[order - i]))).append(" * x")
						.append(i == 1 ? "" : "^" + i);
			}

		}
		// Remove the extra "*x^0 + ";
		if (sb.lastIndexOf("^0") > 0) {
			sb.setLength(sb.length() - 6);
		}
		return sb.toString();
	}

	public Polynomial derivative() {
		int length = _coefs.length - 1;
		double[] coefficients = new double[length];
		for (int i = 0; i < length; ++i) {
			coefficients[i] = _coefs[i] * (length--);
		}
		return new Polynomial(coefficients);
	}

	/***
	 * Get polynomial coefficients
	 * @return Array containing the coefficients in descending order
	 */
	public double[] getCoefficients() {
		return Arrays.copyOf(_coefs, _coefs.length);
	}

	/***
	 * Evaluates the polynomial at x using Horner's method
	 * 
	 * @param x
	 * @return The value of the polynomial at x
	 */
	@Override
	public double evaluateAt(double x) {
		// Horner's method
		double result = 0;
		for (double coefficient : this._coefs) {
			result = result * x + coefficient;
		}
		return result;
	}

	public double[] evaluateAt(double[] x) {
		// Horner's method
		double[] result = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			for (double coefficient : this._coefs) {
				result[i] = result[i] * x[i] + coefficient;
			}
		}
		return result;
	}
	
	/***		
	 * Evaluates the polynomial at real + j * imag using Horner's method
	 * 
	 * @param real part of the complex number
	 * @param imaginary part of the complex number
	 * @return The value of the polynomial at real + j * imag
	 */
	public Complex evaluateAt(double real, double imag) {
		// Horner's method
		Complex result = new Complex();
		for (double coef : _coefs) {
			result.multiplyEquals(real, imag);
			result.addEquals(coef);
		}
		return result;
	}

	public Complex[] roots() {
		if (_roots == null) {
			int N = _coefs.length - 1;
			_roots = new Complex[N];
			switch (N) {
			case 0:
				_roots = new Complex[0];
				break;
			case 1:
				_roots = new Complex[] { new Complex(-_coefs[1] / _coefs[0], 0) };
				break;
			case 2:
				_roots = Polynomials.quadraticFormula(_coefs[0], _coefs[1], _coefs[2]);
				break;
			case 3:
				_roots = Polynomials.cubicFormula(_coefs[0], _coefs[1], _coefs[2], _coefs[3]);
				break;
			default:
				// Use generalized eigenvalue decomposition to find the roots
				Matrix c = Matrices.Companion(_coefs, N);
				EigenvalueDecomposition evd = c.eig();
				double[] realEig = evd.getRealEigenvalues();
				double[] imagEig = evd.getImagEigenvalues();
				for (int i = 0; i < N; i++) {
					_roots[N - i - 1] = new Complex(realEig[i], imagEig[i]);
				}
			}
		}
		Complex[] result = new Complex[_roots.length];
		for(int i = 0; i < _roots.length; ++i) {
			result[i] = new Complex(_roots[i]);
		}
		return result;
	}
	
	/***
	 * Scales polynomial coefficients
	 * @param d
	 */
	public void substituteEquals(double d) {
		final int deg = this.degree();
		for(int i = 0; i < deg; ++i) {
			for(int j = i; j < deg; ++j) {
				_coefs[i] *= d;
			}
		}
		_roots = null;
	}
	
	/***
	 * Scales polynomial coefficients
	 * @param d
	 */
	public Polynomial substitute(double d) {
		final int deg = this.degree();
		double[] result = Arrays.copyOf(_coefs, _coefs.length);
		for(int i = 0; i < deg; ++i) {
			for(int j = i; j < deg; ++j) {
				result[i] *= d;
			}
		}
		return new Polynomial(result);
	}
	
	public void substituteEquals(Polynomial p) {
		Polynomial result = substituteOp(this, p);
		_coefs = result._coefs;
		_roots = null;
	}

	
	public Polynomial substitute(Polynomial p) {
		return substituteOp(this, p);
	}
	
	private static Polynomial substituteOp(Polynomial src, Polynomial sub) {
		final int deg = src.degree();
		Polynomial result = sub.pow(deg);
		result.multiplyEquals(src._coefs[0]);
		Polynomial tmp = null;
		for(int i = deg - 1, j = 1; i >= 0; --i, ++j) {
			tmp = sub.pow(i);
			tmp.multiplyEquals(src._coefs[j]);
			result.addEquals(tmp);
		}
		return result;
	}
	
	
	public Polynomial pow(int n) {
		if(n < 0) {
			throw new IllegalArgumentException("Power must be >= 0");
		}
		if(n == 0) {
			return new Polynomial(new double[] { 1.0 });
		}
		double[] tmp = Arrays.copyOf(_coefs, _coefs.length);
		while(--n > 0) {
			tmp = ArrayUtils.conv(tmp, _coefs);
		}
		return new Polynomial(tmp);
	}

	public double getCoefficientAt(int index) {
		return _coefs[index];
	}

	public static void main(String[] args) {
		// everything here is correct. It just needs to
		// be moved to an unit test
		double[] x = new double[] { 1, 2, 3, 4 };
		double[] y = new double[] { 1, 10, 12, 15 };
		Polynomial poly = Polynomials.polyfit(x, y, 2);
		Polynomial poly2 = Polynomials.polyfit(x, y, 3);
		Polynomial poly3 = Polynomials.polyfit(x, y, 4);

		System.out.println(poly.evaluateAt(2.5));
		System.out.println(poly2.evaluateAt(2.5));
		System.out.println(poly3.evaluateAt(2.5));

		System.out.println(poly);
		System.out.println(poly2);
		System.out.println(poly3);
		
		System.out.println(Arrays.toString(poly.roots()));
		System.out.println(Arrays.toString(poly2.roots()));
		System.out.println(Arrays.toString(poly3.roots()));
		
		System.out.println(Arrays.toString(new Polynomial(new double[] {1, 1, 1}).roots()));
		System.out.println(Arrays.toString(new Polynomial(new double[] {1, 2, 1}).roots()));
		
		System.out.println(Arrays.toString(new Polynomial(new double[] {8, 6, 7, 5, 3, 0, 9}).roots()));
	}
}